package org.uniprot.api.idmapping.service.job;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;

import java.util.concurrent.Executor;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Configuration
@EnableAsync
public class SpringAsyncConfig implements AsyncConfigurer {
    private final IdMappingJobCacheService cacheService;
    @Autowired
    public SpringAsyncConfig(IdMappingJobCacheService cacheService){
        this.cacheService = cacheService;
    }

    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncJobSubmitExceptionHandler(this.cacheService);
    }
}
