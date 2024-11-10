package net.devnguyen.hermes.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.service.WorkerAccountLocker;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Slf4j
public class SaveOffsetsOnRebalance implements ConsumerRebalanceListener {

    @Autowired
    private WorkerAccountLocker workerAccountLocker;

    @Value("${kafka-config.operation-account.topic-process}")
    private String topic;

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
        collection.forEach(partition -> {
            if (partition.topic().equals(topic)) {
                workerAccountLocker.kafkaRevokePartition(partition.partition() + "");
                log.info("Revoked partition: {}", partition);
            }
        });
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {
        collection.forEach(partition -> {
            if (partition.topic().equals(topic)) {
                log.info("Received assigned partition: {}", partition);
                workerAccountLocker.kafkaAssignedPartition(partition.partition() + "");
            }
        });
    }



    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        ConsumerRebalanceListener.super.onPartitionsLost(partitions);
    }

    @PreDestroy
    public void shutdown() {
        workerAccountLocker.kafkaDelPartition();
    }
}
