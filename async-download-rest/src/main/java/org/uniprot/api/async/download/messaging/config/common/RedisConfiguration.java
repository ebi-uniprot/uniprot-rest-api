package org.uniprot.api.async.download.messaging.config.common;

import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.uniprot.api.async.download.model.common.DownloadJob;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@Configuration
@EnableRedisRepositories(basePackages = "org.uniprot.api.async.download.messaging.repository")
public class RedisConfiguration {

    @Value("${redis.hash.prefix}")
    private String redisHashPrefix;

    public String getRedisHashPrefix() {
        return redisHashPrefix;
    }

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    public RedisTemplate<String, DownloadJob> redisTemplate(
            RedissonConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, DownloadJob> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        return template;
    }
}
