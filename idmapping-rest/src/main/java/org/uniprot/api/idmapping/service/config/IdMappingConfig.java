package org.uniprot.api.idmapping.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.concurrency.StreamConcurrencyProperties;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.EhCacheMappingJobService;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@Configuration
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
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PIR_ID_MAPPING_CACHE);
    }

    @Bean
    @Profile("live")
    public IdMappingJobCacheService cacheService(CacheManager cacheManager) {
        Cache mappingCache = cacheManager.getCache(PIR_ID_MAPPING_CACHE);
        return new EhCacheMappingJobService(mappingCache);
    }

    @Bean
    public ThreadPoolTaskExecutor configurableJobTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    public void setTaskExecutorProperties(StreamConcurrencyProperties taskExecutorProperties) {
        this.taskExecutorProperties = taskExecutorProperties;
    }
}
