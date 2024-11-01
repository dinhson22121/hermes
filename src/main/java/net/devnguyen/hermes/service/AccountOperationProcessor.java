package net.devnguyen.hermes.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.HVirtualThreadExecutor;
import net.devnguyen.hermes.HermesApplication;
import net.devnguyen.hermes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class AccountOperationProcessor {
    @Autowired
    private AccountService accountService;

    @Autowired
    private WorkerAccountLocker workerAccountLocker;

    public AccountOperationProcessor() {
        startMainThread();
    }

    public AtomicLong counter = new AtomicLong(0);

    //    private static final int MAX_PENDING_EVENTS = 10;
    private static final int MAX_PENDING_EVENTS = 10;
    private static final int MAX_RUNNING_THREADS = 1_000_000;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, BlockingQueue<AccountOperation>> accountOperationQueues = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<AccountTask> pendingUsersQueue = new PriorityBlockingQueue<>();
    private final ConcurrentHashMap<String, AtomicBoolean> userProcessingStatus = new ConcurrentHashMap<>();
    private final HVirtualThreadExecutor threadPool = new HVirtualThreadExecutor(MAX_RUNNING_THREADS);  // Virtual Thread Pool
    private final Semaphore semaphore = new Semaphore(0);  // Điều khiển theo event-driven

    // Thêm sự kiện cho user
    public boolean addEvent(AccountOperation accountOperation) {
        if (threadPool.isOverload()) {
            log.warn("Thread pool is overload");
            return false;
        }

        BlockingQueue<AccountOperation> queue = accountOperationQueues
                .computeIfAbsent(accountOperation.getAccountId(), k -> new LinkedBlockingQueue<>(MAX_PENDING_EVENTS));


        if (queue.size() >= MAX_PENDING_EVENTS) {
            log.warn("Event rejected due to queue limit for user: {}", accountOperation.getAccountId());
            return false;
        }

        if (!workerAccountLocker.pickAccount(HermesApplication.ID, accountOperation.getAccountId())) {
            log.warn("Event rejected because account working in other worker: {}", accountOperation.getAccountId());
            return false;
        }

        // dung redis check duplicate operation
        // and insert operation to mongo
        if (!queue.offer(accountOperation)) {
            log.error("Event rejected due to queue limit: {}", accountOperation.getAccountId());
            return false;
        }


        AtomicBoolean isProcessing = userProcessingStatus.computeIfAbsent(accountOperation.getAccountId(), k -> new AtomicBoolean(false));

        // Nếu user chưa trong hàng đợi chờ xử lý, thêm vào và báo hiệu có sự kiện
        if (isProcessing.compareAndSet(false, true)) {
            pendingUsersQueue.offer(new AccountTask(accountOperation.getAccountId(), queue.size()));
            semaphore.release();
        }

        return true;
    }

    public void startMainThread() {
        log.info("startMainThread");
        if (running.compareAndSet(false, true)) {
            Executors.newSingleThreadExecutor().submit(() -> {
                log.info("main executor running");
                while (true) {
                    semaphore.acquire();  // Đợi có sự kiện mới

                    AccountTask accountTask = pendingUsersQueue.poll();
//                log.info("Process AccountTask: {}", accountTask);
                    if (accountTask == null) {
                        continue;
                    }

                    // list thread
                    threadPool.submit(() -> {
                        processUserEvents(accountTask.accountId());
                    });
                }
            });
        }
    }

    // Phương thức xử lý sự kiện của một user trong ThreadPool
    private void processUserEvents(String userId) {
//        log.info("Processing user events for {}", userId);
        BlockingQueue<AccountOperation> queue = accountOperationQueues.get(userId);

        try {
            while (!queue.isEmpty()) {
                // holdNodeId
                /*
                    if hold false
                        throw exception
                 */
                processEvent(queue.poll());
            }
        } finally {
            // redis.release(account_id)
            AtomicBoolean processing = userProcessingStatus.computeIfAbsent(userId, k -> new AtomicBoolean(false));
            processing.compareAndSet(true, false);

            if (!queue.isEmpty() && processing.compareAndSet(false, true)) {
                pendingUsersQueue.offer(new AccountTask(userId, queue.size()));
                semaphore.release();
            }
        }
    }

    // Xử lý từng event
    private void processEvent(AccountOperation accountOperation) {
        counter.incrementAndGet();
        accountService.incrBalance(accountOperation.getAccountId(), BigDecimal.valueOf(1));
    }

    @PreDestroy
    public void shutdown() {
        log.info("shutdown");
        threadPool.shutdown();
        workerAccountLocker.shutdown(HermesApplication.ID);
        //redis.destroy
    }
}
