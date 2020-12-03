package org.uniprot.api.uniparc.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

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
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.api.uniparc.output.converter.*;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

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

                ReturnFieldConfig returnFieldConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC);

                converters.add(new UniParcFastaMessageConverter());
                converters.add(
                        new TsvMessageConverter<>(
                                UniParcEntry.class,
                                returnFieldConfig,
                                new UniParcEntryValueMapper()));
                converters.add(
                        new XlsMessageConverter<>(
                                UniParcEntry.class,
                                returnFieldConfig,
                                new UniParcEntryValueMapper()));

                JsonMessageConverter<UniParcEntry> uniparcJsonConverter =
                        new JsonMessageConverter<>(
                                UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniParcEntry.class,
                                returnFieldConfig);
                converters.add(0, uniparcJsonConverter);
                converters.add(1, new UniParcXmlMessageConverter(""));
                converters.add(new RDFMessageConverter());
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
                        uniparcContext(XLS_MEDIA_TYPE),
                        uniparcContext(RDF_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniParcEntry> uniparcContext(MediaType contentType) {
        return MessageConverterContext.<UniParcEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .build();
    }
}
