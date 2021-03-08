package org.uniprot.api.idmapping.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.common.concurrency.TaskExecutorProperties;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Configuration
@ConfigurationProperties(prefix = "id.mapping.job")
public class JobConfig {
    private final TaskExecutorProperties taskExecutorProperties = new TaskExecutorProperties();

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
    public ThreadPoolTaskExecutor configurableJobTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
