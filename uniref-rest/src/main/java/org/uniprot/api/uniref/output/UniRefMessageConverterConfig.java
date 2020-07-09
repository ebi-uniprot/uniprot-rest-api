package org.uniprot.api.uniref.output;

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
import org.uniprot.api.uniref.output.converter.*;
import org.uniprot.core.json.parser.uniref.UniRefEntryJsonConfig;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryValueMapper;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
@Configuration
@ConfigurationProperties(prefix = "download")
@Getter
@Setter
public class UniRefMessageConverterConfig {
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
                ReturnFieldConfig returnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF);

                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(new ListMessageConverter());
                converters.add(new UniRefFastaMessageConverter());
                converters.add(
                        new TsvMessageConverter<>(
                                UniRefEntry.class, returnConfig, new UniRefEntryValueMapper()));
                converters.add(
                        new XlsMessageConverter<>(
                                UniRefEntry.class, returnConfig, new UniRefEntryValueMapper()));

                JsonMessageConverter<UniRefEntry> unirefJsonMessageConverter =
                        new JsonMessageConverter<>(
                                UniRefEntryJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniRefEntry.class,
                                returnConfig);
                converters.add(0, unirefJsonMessageConverter);
                converters.add(1, new UniRefXmlMessageConverter("", ""));
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<UniRefEntryLight>
            uniRefLightMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefEntryLight> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniRefLightContext(LIST_MEDIA_TYPE),
                        uniRefLightContext(APPLICATION_XML),
                        uniRefLightContext(APPLICATION_JSON),
                        uniRefLightContext(FASTA_MEDIA_TYPE),
                        uniRefLightContext(TSV_MEDIA_TYPE),
                        uniRefLightContext(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniRefEntry> uniRefMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniRefContext(LIST_MEDIA_TYPE),
                        uniRefContext(APPLICATION_XML),
                        uniRefContext(APPLICATION_JSON),
                        uniRefContext(FASTA_MEDIA_TYPE),
                        uniRefContext(TSV_MEDIA_TYPE),
                        uniRefContext(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniRefEntry> uniRefContext(MediaType contentType) {
        return MessageConverterContext.<UniRefEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIREF)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniRefEntryLight> uniRefLightContext(MediaType contentType) {
        return MessageConverterContext.<UniRefEntryLight>builder()
                .resource(MessageConverterContextFactory.Resource.UNIREF)
                .contentType(contentType)
                .build();
    }
}
