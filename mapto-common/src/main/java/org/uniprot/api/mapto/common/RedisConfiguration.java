package org.uniprot.api.mapto.common;

import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.uniprot.api.mapto.common.model.MapToJob;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@Configuration
@EnableRedisRepositories(basePackages = "org.uniprot.api.mapto.common.repository")
public class RedisConfiguration {

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    public RedisTemplate<String, MapToJob> redisTemplate(
            RedissonConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, MapToJob> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        return template;
    }
}
