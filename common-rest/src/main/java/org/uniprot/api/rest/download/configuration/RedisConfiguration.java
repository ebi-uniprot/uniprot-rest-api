package org.uniprot.api.rest.download.configuration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.uniprot.api.rest.download.model.DownloadJob;

import java.io.IOException;

/**
 * TODO check possible duplicate code in {@link
 * org.uniprot.api.idmapping.service.config.IdMappingConfig}
 *
 * @author sahmad
 * @created 22/12/2022
 */
@Configuration
@EnableRedisRepositories(basePackages = "org.uniprot.api.rest.download.repository")
public class RedisConfiguration {
    @Bean
    @Profile("live")
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("live")
    public RedissonClient redisson(@Value("classpath:/redisson.yaml") Resource configFile)
            throws IOException {
        Config config = Config.fromYAML(configFile.getInputStream());
        RedissonClient client = Redisson.create(config);
        return client;
    }

    @Bean
    public RedisTemplate<String, DownloadJob> redisTemplate(
            RedissonConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, DownloadJob> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        return template;
    }
}
