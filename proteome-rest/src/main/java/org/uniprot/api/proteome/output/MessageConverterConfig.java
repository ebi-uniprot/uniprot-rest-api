package org.uniprot.api.proteome.output;

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
import org.uniprot.api.proteome.output.converter.*;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.core.json.parser.genecentric.GeneCentricJsonConfig;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.parser.tsv.proteome.ProteomeEntryValueMapper;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

/**
 * @author jluo
 * @date: 29 Apr 2019
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
                converters.add(new GeneCentricFastaMessageConverter());

                ReturnFieldConfig returnFieldConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.PROTEOME);
                converters.add(
                        new TsvMessageConverter<>(
                                ProteomeEntry.class,
                                returnFieldConfig,
                                new ProteomeEntryValueMapper()));
                converters.add(
                        new XlsMessageConverter<>(
                                ProteomeEntry.class,
                                returnFieldConfig,
                                new ProteomeEntryValueMapper()));

                JsonMessageConverter<ProteomeEntry> proteomeJsonConverter =
                        new JsonMessageConverter<>(
                                ProteomeJsonConfig.getInstance().getSimpleObjectMapper(),
                                ProteomeEntry.class,
                                returnFieldConfig);
                converters.add(0, proteomeJsonConverter);
                converters.add(1, new ProteomeXmlMessageConverter());

                JsonMessageConverter<GeneCentricEntry> geneCentricJsonConverter =
                        new JsonMessageConverter<>(
                                GeneCentricJsonConfig.getInstance().getSimpleObjectMapper(),
                                GeneCentricEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.GENECENTRIC));
                converters.add(0, geneCentricJsonConverter);
                converters.add(1, new GeneCentricXmlMessageConverter());
            }
        };
    }

    @Bean(name = "PROTEOME")
    public MessageConverterContextFactory<ProteomeEntry> proteomeMessageConverterContextFactory() {
        MessageConverterContextFactory<ProteomeEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        proteomeContext(LIST_MEDIA_TYPE),
                        proteomeContext(APPLICATION_XML),
                        proteomeContext(APPLICATION_JSON),
                        proteomeContext(TSV_MEDIA_TYPE),
                        proteomeContext(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<ProteomeEntry> proteomeContext(MediaType contentType) {
        return MessageConverterContext.<ProteomeEntry>builder()
                .resource(MessageConverterContextFactory.Resource.PROTEOME)
                .contentType(contentType)
                .build();
    }

    @Bean(name = "GENECENTRIC")
    public MessageConverterContextFactory<GeneCentricEntry>
            geneCentricMssageConverterContextFactory() {
        MessageConverterContextFactory<GeneCentricEntry> contextFactory =
                new MessageConverterContextFactory<>();
        asList(
                        geneCentricContent(LIST_MEDIA_TYPE),
                        geneCentricContent(APPLICATION_XML),
                        geneCentricContent(APPLICATION_JSON),
                        geneCentricContent(FASTA_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<GeneCentricEntry> geneCentricContent(MediaType contentType) {
        return MessageConverterContext.<GeneCentricEntry>builder()
                .resource(MessageConverterContextFactory.Resource.GENECENTRIC)
                .contentType(contentType)
                .build();
    }
}
