package net.devnguyen.hermes.kafka;

import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.dto.AccountOperationDTO;
import net.devnguyen.hermes.service.WorkerAccountLocker;
import net.devnguyen.hermes.utils.Utils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OperationAccountProducer {
    @Value("${kafka-config.operation-account.topic-process}")
    private String topic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    WorkerAccountLocker workerAccountLocker;

    Map<Integer, AtomicLong> partitionOffsets = new ConcurrentHashMap<>();

    AtomicLong routerCounter = new AtomicLong(0L);

    AdminClient adminClient;


    public OperationAccountProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        var properties = kafkaTemplate.getProducerFactory().getConfigurationProperties();
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
        this.adminClient = AdminClient.create(props);

//        build();
    }

    public CompletableFuture senOperation(AccountOperationDTO operation) {
        var json = operation.toJson();

        var defaultPartitionId = getBestPartitionId();

        var response = workerAccountLocker.getPartitionRouterForAccount(operation.getAccountId(), defaultPartitionId, false);
        if (response.isNotOk()) {
            log.error("cannot router operation, rollback or return false operation");
            return null;
        }
        Integer partitionId = response.getData();
        partitionOffsets.get(partitionId).incrementAndGet();
        return kafkaTemplate.send(topic, partitionId, operation.getId(), json);
    }


    @Scheduled(fixedDelay = 10 * 1000)
    public void build() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            KafkaFuture<Map<String, TopicDescription>> tmp = adminClient.describeTopics(Collections.singletonList(topic)).allTopicNames();

            TopicDescription topicDescription = tmp.get().get(topic);

            var topicPartitions = topicDescription.partitions().stream().map(x -> new TopicPartition(topic, x.partition())).toList();

            Map<TopicPartition, OffsetSpec> map = topicPartitions.stream().collect(Collectors.toMap(x -> x, x -> OffsetSpec.latest()));

            // Get the end offset for the partition
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets =
                    adminClient.listOffsets(map).all().get();


            offsets.forEach((p, o) -> {
                partitionOffsets.put(p.partition(), new AtomicLong(o.offset()));
            });
            routerCounter.set(0);
        } catch (Exception e) {
            log.error("failed to build operation account", e);
        } finally {
            stopWatch.stop();
            log.info("data: {}", partitionOffsets);
            log.info("build partitionOffsets execution time: {} ms", stopWatch.getTotalTimeMillis());
        }
    }


    public int getBestPartitionId() {
//        if (partitionOffsets.isEmpty()) {
//            build();
//        }

        AtomicInteger partitionId = new AtomicInteger();
        int offset = -1;
        partitionOffsets.forEach((p, o) -> {
            if (offset < o.get()) {
                partitionId.set(p);
            }
        });

        return partitionId.get();
    }


    @KafkaListener(containerFactory = "kafkaListenerContainerFactory", topics = "${kafka-config.operation-account.topic-request}", groupId = "${kafka-config.operation-account.group-id}")
    public void listen(ConsumerRecord<String, String> record) {
        String value = record.value();
        senOperation(AccountOperationDTO.readValue(value));
    }

}
