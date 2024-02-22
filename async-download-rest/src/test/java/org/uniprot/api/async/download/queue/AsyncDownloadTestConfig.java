package org.uniprot.api.async.download.queue;

import java.io.IOException;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("offline & asyncDownload")
public class AsyncDownloadTestConfig {
    @Bean
    public RedissonClient redisson() throws IOException {
        Config config = new Config();
        String host = System.getProperty("uniprot.redis.host");
        String port = System.getProperty("uniprot.redis.port");
        config.useSingleServer().setAddress("redis://" + host + ":" + port).setPassword(null);
        RedissonClient client = Redisson.create(config);
        return client;
    }
}
