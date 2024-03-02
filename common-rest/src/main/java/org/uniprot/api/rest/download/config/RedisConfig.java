package org.uniprot.api.rest.download.config;

import java.io.IOException;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@Configuration
public class RedisConfig {
    @Bean(destroyMethod = "shutdown")
    @Profile("asyncDownload & live")
    public RedissonClient redisson(@Value("${download.redis.config.file}") Resource configFile)
            throws IOException {
        Config config = Config.fromYAML(configFile.getInputStream());
        return Redisson.create(config);
    }
}
