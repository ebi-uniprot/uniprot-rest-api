package org.uniprot.api.rest.output;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.concurrency.StreamConcurrencyProperties;

/**
 * Created 25/02/2022
 *
 * @author Edd
 */
@Configuration
@ConfigurationProperties(prefix = "download")
public class StreamedRequestsConfig {
    private StreamConcurrencyProperties streamProperties = new StreamConcurrencyProperties();

    @Bean
    public ThreadPoolTaskExecutor downloadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(streamProperties.getCorePoolSize());
        executor.setMaxPoolSize(streamProperties.getMaxPoolSize());
        executor.setQueueCapacity(streamProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(streamProperties.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(streamProperties.isAllowCoreThreadTimeout());
        executor.setWaitForTasksToCompleteOnShutdown(
                streamProperties.isWaitForTasksToCompleteOnShutdown());
        // initialize() is important because it forces the task executor to change
        // the properties used (e.g., otherwise the queue capacity defaults to Integer.MAX_VALUE)
        executor.initialize();
        return executor;
    }

    @Bean
    public Gatekeeper downloadGatekeeper() {
        return new Gatekeeper(
                streamProperties.getConcurrentLargeDownloadsCount(),
                streamProperties.getTimeoutInSecondsForLargeDownloads());
    }

    public void setStreamProperties(StreamConcurrencyProperties streamProperties) {
        this.streamProperties = streamProperties;
    }
}
