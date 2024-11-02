package net.devnguyen.hermes.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.mongodb.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
@EnableConfigurationProperties(MongoThreadPoolProperties.class)
public class MongoConfig {

    @Bean
    // spring mongodb current not support custom config connect pool - manual setting
    public MongoClientSettings getMongoClientSettings(MongoThreadPoolProperties mongoThreadPoolProperties, MongoProperties properties) {
        return MongoClientSettings.builder()
                .applicationName("hermes")
                .applyConnectionString(new ConnectionString(properties.getUri()))
                .applyToSslSettings(sslBuilder ->
                        sslBuilder.
                                enabled(properties.getSsl().isEnabled()).
                                invalidHostNameAllowed(false))
                .applyToConnectionPoolSettings(connPoolBuilder ->
                        connPoolBuilder.maxWaitTime(mongoThreadPoolProperties.getMaxWaitTime(), MILLISECONDS)
                                .minSize(mongoThreadPoolProperties.getMinSize())
                                .maxSize(mongoThreadPoolProperties.getMaxSize())
                                .maxConnectionIdleTime(mongoThreadPoolProperties.getMaxConnectionIdleTime(), MILLISECONDS)
                )
                .applyToSocketSettings(socketBuilder ->
                        socketBuilder.
                                connectTimeout(60_000, MILLISECONDS))
                .readPreference(ReadPreference.secondaryPreferred())
                .build();
    }
}
