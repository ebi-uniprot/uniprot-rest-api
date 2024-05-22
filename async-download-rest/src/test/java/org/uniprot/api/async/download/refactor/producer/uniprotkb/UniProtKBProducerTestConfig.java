package org.uniprot.api.async.download.refactor.producer.uniprotkb;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration
@ComponentScan({"org.uniprot.api.async.download.refactor.producer.uniprotkb",
        "org.uniprot.api.async.download.refactor.messaging.uniprotkb",
        "org.uniprot.api.async.download.refactor.service.uniprotkb",
        "org.uniprot.api.async.download.refactor.consumer.uniprotkb",
        "org.uniprot.api.async.download.messaging.config.uniprotkb",
        "org.uniprot.api.async.download.messaging.result.uniprotkb",
        "org.uniprot.api.async.download.messaging.listener.common",
        "org.uniprot.api.async.download.messaging.listener.uniprotkb",
        "org.uniprot.api.async.download.messaging.producer.uniprotkb"})
@Testcontainers
class UniProtKBProducerTestConfig {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+System.getProperty("uniprot.redis.host")+":"+System.getProperty("uniprot.redis.port"));
        return Redisson.create(config);
    }

}
