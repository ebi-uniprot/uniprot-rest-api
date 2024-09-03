package org.uniprot.api.idmapping.common.service.config;

import java.io.IOException;
import java.util.Map;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.concurrency.StreamConcurrencyProperties;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.impl.RedisCacheMappingJobService;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "id.mapping.job")
public class IdMappingConfig {
    private static final String PIR_ID_MAPPING_CACHE = "pirIDMappingCache";

    private StreamConcurrencyProperties taskExecutorProperties = new StreamConcurrencyProperties();

    @Bean
    @Profile("live")
    public ThreadPoolTaskExecutor jobTaskExecutor(
            ThreadPoolTaskExecutor configurableJobTaskExecutor) {
        configurableJobTaskExecutor.setCorePoolSize(taskExecutorProperties.getCorePoolSize());
        configurableJobTaskExecutor.setMaxPoolSize(taskExecutorProperties.getMaxPoolSize());
        configurableJobTaskExecutor.setQueueCapacity(taskExecutorProperties.getQueueCapacity());
        configurableJobTaskExecutor.setKeepAliveSeconds(
                taskExecutorProperties.getKeepAliveSeconds());
        configurableJobTaskExecutor.setAllowCoreThreadTimeOut(
                taskExecutorProperties.isAllowCoreThreadTimeout());
        configurableJobTaskExecutor.setWaitForTasksToCompleteOnShutdown(
                taskExecutorProperties.isWaitForTasksToCompleteOnShutdown());
        return configurableJobTaskExecutor;
    }

    @Bean
    public RestTemplate idMappingRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    @Profile("live")
    public IdMappingJobCacheService idMappingJobCacheService(
            @Value("${id.mapping.cache.config.file}") Resource cacheConfig,
            RedissonClient redissonClient)
            throws IOException {

        Map<String, CacheConfig> config =
                (Map<String, CacheConfig>) CacheConfig.fromYAML(cacheConfig.getInputStream());
        CacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
        Cache mappingCache = cacheManager.getCache(PIR_ID_MAPPING_CACHE);
        return new RedisCacheMappingJobService(mappingCache);
    }

    public void setTaskExecutorProperties(StreamConcurrencyProperties taskExecutorProperties) {
        this.taskExecutorProperties = taskExecutorProperties;
    }
}
