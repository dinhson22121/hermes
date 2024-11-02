package net.devnguyen.hermes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.mongodb.connection-pool")
@Data
public class MongoThreadPoolProperties {
    private int maxSize = 100;
    private int minSize;
    private long maxWaitTime = 120_000;
    private long maxConnectionIdleTime = 120_000L;
}
