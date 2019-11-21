package org.uniprot.api.uniparc.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.TaskExecutorProperties;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.ErrorMessageConverter;
import org.uniprot.api.rest.output.converter.ErrorMessageXMLConverter;
import org.uniprot.api.rest.output.converter.ListMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcFastaMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcJsonMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcTsvMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcXmlMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcXslMessageConverter;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @author jluo
 * @date: 25 Jun 2019
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

    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(new ListMessageConverter());
                converters.add(new UniParcFastaMessageConverter());
                converters.add(new UniParcTsvMessageConverter());
                converters.add(new UniParcXslMessageConverter());

                converters.add(0, new UniParcJsonMessageConverter());
                converters.add(1, new UniParcXmlMessageConverter(""));
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<UniParcEntry> uniparcMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniparcContext(LIST_MEDIA_TYPE),
                        uniparcContext(APPLICATION_XML),
                        uniparcContext(APPLICATION_JSON),
                        uniparcContext(FASTA_MEDIA_TYPE),
                        uniparcContext(TSV_MEDIA_TYPE),
                        uniparcContext(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniParcEntry> uniparcContext(MediaType contentType) {
        return MessageConverterContext.<UniParcEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .clazz(UniParcEntry.class)
                .build();
    }
}
