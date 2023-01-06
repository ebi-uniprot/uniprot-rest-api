package org.uniprot.api.rest.download.repository;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import redis.embedded.RedisServer;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@TestConfiguration
public class TestConfig {
    private final RedisServer redisServer;

    public TestConfig() {
        this.redisServer = new RedisServer(6370);
    }

    @PostConstruct
    public void postConstruct() {
        try {
            this.redisServer.start();
        } catch (RuntimeException rte) {
            // already running
        }
    }

    @PreDestroy
    public void preDestroy() {
        this.redisServer.stop();
    }

    @Bean
    @Profile("offline")
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("offline")
    RedissonClient redisson() throws IOException {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6370");
        return Redisson.create(config);
    }
}
