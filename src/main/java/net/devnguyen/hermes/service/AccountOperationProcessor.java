package net.devnguyen.hermes.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.HermesApplication;
import net.devnguyen.hermes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
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
    private final ConcurrentHashMap<String, BlockingQueue<AccountOperationDTO>> accountOperationQueues = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<AccountTask> pendingUsersQueue = new PriorityBlockingQueue<>();
    private final ConcurrentHashMap<String, AtomicBoolean> userProcessingStatus = new ConcurrentHashMap<>();
    private final HVirtualThreadExecutor threadPool = new HVirtualThreadExecutor(MAX_RUNNING_THREADS);  // Virtual Thread Pool
    private final Semaphore semaphore = new Semaphore(0);  // Điều khiển theo event-driven

    // Thêm sự kiện cho user
    public boolean addEvent(AccountOperationDTO accountOperationDTO) {
        if (threadPool.isOverload()) {
            log.warn("Thread pool is overload");
            return false;
        }

        BlockingQueue<AccountOperationDTO> queue = accountOperationQueues
                .computeIfAbsent(accountOperationDTO.getAccountId(), k -> new LinkedBlockingQueue<>(MAX_PENDING_EVENTS));

        if (queue.size() >= MAX_PENDING_EVENTS) {
            log.warn("Event rejected due to queue limit for user: {}", accountOperationDTO.getAccountId());
            return false;
        }

        if (!workerAccountLocker.pickAccount(accountOperationDTO.getAccountId())) {
            log.warn("Event rejected because account working in other worker: {}", accountOperationDTO.getAccountId());
            return false;
        }

        // dung redis check duplicate operation
        // and insert operation to mongo
        if (!queue.offer(accountOperationDTO)) {
            log.error("Event rejected due to queue limit: {}", accountOperationDTO.getAccountId());
            return false;
        }

        AtomicBoolean isProcessing = userProcessingStatus.computeIfAbsent(accountOperationDTO.getAccountId(), k -> new AtomicBoolean(false));

        // Nếu user chưa trong hàng đợi chờ xử lý, thêm vào và báo hiệu có sự kiện
        if (isProcessing.compareAndSet(false, true)) {
            pendingUsersQueue.offer(new AccountTask(accountOperationDTO.getAccountId(), queue.size()));
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
    private void processUserEvents(String accountId) {
        BlockingQueue<AccountOperationDTO> queue = accountOperationQueues.get(accountId);

        try {
            while (!queue.isEmpty()) {
                if (!workerAccountLocker.pickAccount(accountId)) {
                    //stop all queue and reject operation
                    log.error("operation break because other worker picked account");
                }
                processEvent(queue.poll());
            }
        } finally {
//            workerAccountLocker.unpickAccount(HermesApplication.ID, accountId);

            AtomicBoolean processing = userProcessingStatus.computeIfAbsent(accountId, k -> new AtomicBoolean(false));
            processing.compareAndSet(true, false);

            if (!queue.isEmpty() && processing.compareAndSet(false, true)) {
                pendingUsersQueue.offer(new AccountTask(accountId, queue.size()));
                semaphore.release();
            }
        }
    }

    // Xử lý từng event
    private void processEvent(AccountOperationDTO accountOperationDTO) {
        counter.incrementAndGet();
        accountService.incrBalance(accountOperationDTO.getAccountId(), BigDecimal.valueOf(1), accountOperationDTO.getCreatedAt());
    }

    @PreDestroy
    public void shutdown() {
        log.info("shutdown");
        threadPool.shutdown();
        workerAccountLocker.shutdown();
        //redis.destroy
    }
}
