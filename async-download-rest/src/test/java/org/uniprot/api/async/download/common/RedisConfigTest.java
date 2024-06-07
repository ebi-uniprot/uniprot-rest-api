package org.uniprot.api.async.download.common;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class RedisConfigTest {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+System.getProperty("uniprot.redis.host")+":"+System.getProperty("uniprot.redis.port"));
        return Redisson.create(config);
    }
}
