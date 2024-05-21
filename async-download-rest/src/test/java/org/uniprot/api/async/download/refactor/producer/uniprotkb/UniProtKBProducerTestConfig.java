package org.uniprot.api.async.download.refactor.producer.uniprotkb;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.Objects;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestConfiguration
@ComponentScan({"org.uniprot.api.async.download.refactor.producer.uniprotkb",
        "org.uniprot.api.async.download.refactor.messaging.uniprotkb",
        "org.uniprot.api.async.download.refactor.service.uniprotkb",
        "org.uniprot.api.async.download.messaging.config.uniprotkb",
        "org.uniprot.api.async.download.messaging.result.uniprotkb",
        "org.uniprot.api.async.download.messaging.listener.common",
        "org.uniprot.api.async.download.messaging.listener.uniprotkb",
        "org.uniprot.api.async.download.messaging.producer.uniprotkb"})
@Testcontainers
class UniProtKBProducerTestConfig {

    @Bean
    public HashGenerator<UniProtKBDownloadRequest> uniProtKBDownloadHashGenerator(
            @Value("${async.download.uniprotkb.hash.salt}") String hashSalt) {
        Function<UniProtKBDownloadRequest, char[]> function = (request) ->  {
            StringBuilder builder = new StringBuilder();
            builder.append(request.getQuery().strip().toLowerCase());
            if (request.hasFields()) {
                builder.append(request.getFields().strip().toLowerCase());
            }
            if (request.hasSort()) {
                builder.append(request.getSort().strip().toLowerCase());
            }

            if (Objects.nonNull(request.getFormat())) {
                builder.append(request.getFormat().toLowerCase());
            }
            return builder.toString().toCharArray();
        };
        return new HashGenerator<>(function, hashSalt);
    }


    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+System.getProperty("uniprot.redis.host")+":"+System.getProperty("uniprot.redis.port"));
        return Redisson.create(config);
    }


}
