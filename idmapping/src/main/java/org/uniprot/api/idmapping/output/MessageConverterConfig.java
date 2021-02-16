package org.uniprot.api.idmapping.output;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.common.concurrency.TaskExecutorProperties;
import org.uniprot.api.idmapping.model.IDMappingPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@ConfigurationProperties(prefix = "download")
@Getter
@Setter
public class MessageConverterConfig {
    private TaskExecutorProperties taskExecutor = new TaskExecutorProperties();

    @Bean
    public ThreadPoolTaskExecutor downloadTaskExecutor(
            ThreadPoolTaskExecutor configurableTaskExecutor) {
        configurableTaskExecutor.setCorePoolSize(taskExecutor.getCorePoolSize());
        configurableTaskExecutor.setMaxPoolSize(taskExecutor.getMaxPoolSize());
        configurableTaskExecutor.setQueueCapacity(taskExecutor.getQueueCapacity());
        configurableTaskExecutor.setKeepAliveSeconds(taskExecutor.getKeepAliveSeconds());
        configurableTaskExecutor.setAllowCoreThreadTimeOut(taskExecutor.isAllowCoreThreadTimeout());
        configurableTaskExecutor.setWaitForTasksToCompleteOnShutdown(
                taskExecutor.isWaitForTasksToCompleteOnShutdown());
        return configurableTaskExecutor;
    }

    @Bean
    public ThreadPoolTaskExecutor configurableTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    @Bean("idMappingMessageConverterContextFactory")
    public MessageConverterContextFactory<IDMappingPair<String>> messageConverterContextFactory() {
        MessageConverterContextFactory<IDMappingPair<String>> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        context(APPLICATION_XML),
                        context(APPLICATION_JSON),
                        context(TSV_MEDIA_TYPE),
                        context(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<IDMappingPair<String>> context(MediaType contentType) {
        return MessageConverterContext.<IDMappingPair<String>>builder()
                .resource(MessageConverterContextFactory.Resource.IDMAPPING_PIR)
                .contentType(contentType)
                .build();
    }
}
