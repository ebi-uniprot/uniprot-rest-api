package org.uniprot.api.idmapping.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.common.concurrency.TaskExecutorProperties;
import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Configuration
@ConfigurationProperties(prefix = "id.mapping.job")
public class JobConfig {
    private final TaskExecutorProperties taskExecutorProperties = new TaskExecutorProperties();

    @Bean
    BlockingQueue<IdMappingJob> jobQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public ThreadPoolTaskExecutor jobTaskExecutor(
            ThreadPoolTaskExecutor configurableJobTaskExecutor) {
        configurableJobTaskExecutor.setCorePoolSize(taskExecutorProperties.getCorePoolSize());
        configurableJobTaskExecutor.setMaxPoolSize(taskExecutorProperties.getMaxPoolSize());
        configurableJobTaskExecutor.setQueueCapacity(taskExecutorProperties.getQueueCapacity());
        configurableJobTaskExecutor.setKeepAliveSeconds(taskExecutorProperties.getKeepAliveSeconds());
        configurableJobTaskExecutor.setAllowCoreThreadTimeOut(taskExecutorProperties.isAllowCoreThreadTimeout());
        configurableJobTaskExecutor.setWaitForTasksToCompleteOnShutdown(
                taskExecutorProperties.isWaitForTasksToCompleteOnShutdown());
        return configurableJobTaskExecutor;
    }

    @Bean
    public ThreadPoolTaskExecutor configurableJobTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

}
