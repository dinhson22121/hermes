package net.devnguyen.hermes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.dto.AccountOperation;
import net.devnguyen.hermes.service.AccountOperationProcessor;
import net.devnguyen.hermes.service.WorkerAccountLocker;
import org.redisson.api.RedissonClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class HermesApplication implements ApplicationListener<ApplicationReadyEvent> {
    public static final String ID = UUID.randomUUID().toString();

    public static void main(String[] args) {
        SpringApplication.run(HermesApplication.class, args);
    }

    private final AccountOperationProcessor accountOperationProcessor;
    private final WorkerAccountLocker workerAccountLocker;

    @Bean
    public ExecutorService taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    private final RedissonClient redissonClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("worker id: {}", ID);
        Executors.newSingleThreadExecutor().submit(() -> {
            log.info("Start stress test");
//            stressTest();
            log.info("End stress test");
        });
    }

    public void stressTest() {
        for (int i = 1; i <= 50_000; ++i) {
            for (int j = 0; j < 10; j++) {
                accountOperationProcessor.addEvent(createEvent("account4-" + i, 1D));
            }
        }
    }

    public static AccountOperation createEvent(String accountId, Double balance) {
        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setAccountId(accountId);
        accountOperation.setBalanceChange(new BigDecimal(balance));
        accountOperation.setCreatedAt(Instant.now());
        accountOperation.setEventId(UUID.randomUUID().toString());
        return accountOperation;
    }

}
