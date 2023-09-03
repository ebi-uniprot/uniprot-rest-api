package org.uniprot.api.rest.download.configuration;

import java.io.IOException;

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

/**
 * @author sahmad
 * @created 22/12/2022
 */
@Configuration
@Profile({"asyncDownload"})
@EnableRedisRepositories(basePackages = "org.uniprot.api.rest.download.repository")
public class RedisConfiguration {

    @Value("${redis.hash.value}")
    private String redisHashValue;

    public String getRedisHashValue() {
        return redisHashValue;
    }

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("asyncDownload & live")
    public RedissonClient redisson(@Value("${download.redis.config.file}") Resource configFile)
            throws IOException {
        Config config = Config.fromYAML(configFile.getInputStream());
        return Redisson.create(config);
    }

    @Bean
    public RedisTemplate<String, DownloadJob> redisTemplate(
            RedissonConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, DownloadJob> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        return template;
    }
}
