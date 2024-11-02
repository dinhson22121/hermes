package net.devnguyen.hermes.kafka;

import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.dto.AccountOperationDTO;
import net.devnguyen.hermes.service.AccountOperationProcessor;
import net.devnguyen.hermes.utils.Utils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.apache.kafka.clients.consumer.Consumer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class OperationAccountConsumer implements ApplicationListener<ApplicationReadyEvent> {
    @Value("${kafka-config.operation-account.topic}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    public void sendOperationTest(String accountId) {


        AccountOperationDTO accountOperationDTO = new AccountOperationDTO();
        accountOperationDTO.setAccountId(accountId);
        accountOperationDTO.setBalance(new BigDecimal(1));
        accountOperationDTO.setCreatedAt(Instant.now());
        accountOperationDTO.setId(UUID.randomUUID().toString());
        try {
            testKafkaTemplate.send(topic, Utils.objectMapper.writeValueAsString(accountOperationDTO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private AccountOperationProcessor accountOperationProcessor;

    @KafkaListener(topics = "${kafka-config.operation-account.topic}", groupId = "${kafka-config.operation-account.group-id}")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
//            log.info("Received record: {}", record);

            var operation = parseOperation(record.value());

            if (!accountOperationProcessor.addEvent(operation)) {
                log.error("Failed to process operation: {}", operation);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
//            rollbackMessage(record, consumer);
        }finally {
            acknowledgment.acknowledge();
        }
    }

    void rollbackMessage(ConsumerRecord<String, String> record, Consumer<?, ?> consumer) {
        TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        consumer.seek(topicPartition, record.offset());
    }

    public AccountOperationDTO parseOperation(String json) {
        try {
            return Utils.objectMapper.readValue(json, AccountOperationDTO.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

    }
}
