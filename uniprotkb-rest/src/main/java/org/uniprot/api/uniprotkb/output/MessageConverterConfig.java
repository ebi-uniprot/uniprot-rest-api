package org.uniprot.api.uniprotkb.output;

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
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.output.converter.*;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

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

    /*
     * Add to the supported message converters.
     * Add more message converters for additional response types.
     */
    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        ReturnFieldConfig returnConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        JsonMessageConverter<UniProtKBEntry> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniProtKBEntry.class,
                        returnConfig);

        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new UniProtKBFlatFileMessageConverter());
                converters.add(new UniProtKBFastaMessageConverter());
                converters.add(new ListMessageConverter());
                converters.add(new RDFMessageConverter());
                converters.add(new UniProtKBGffMessageConverter());
                converters.add(
                        new TsvMessageConverter<>(
                                UniProtKBEntry.class,
                                returnConfig,
                                new UniProtKBEntryValueMapper()));
                converters.add(
                        new XslMessageConverter<>(
                                UniProtKBEntry.class,
                                returnConfig,
                                new UniProtKBEntryValueMapper()));
                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(0, new UniProtKBXmlMessageConverter());
                converters.add(1, jsonMessageConverter);
                converters.add(2, new PublicationJsonMessageConverter());
            }
        };
    }

    @Bean("uniprotMessageConverterContextFactory")
    public MessageConverterContextFactory<UniProtKBEntry> messageConverterContextFactory() {
        MessageConverterContextFactory<UniProtKBEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        context(LIST_MEDIA_TYPE),
                        context(RDF_MEDIA_TYPE),
                        context(FF_MEDIA_TYPE),
                        context(APPLICATION_XML),
                        context(APPLICATION_JSON),
                        context(TSV_MEDIA_TYPE),
                        context(FASTA_MEDIA_TYPE),
                        context(XLS_MEDIA_TYPE),
                        context(GFF_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("publicationMessageConverterContextFactory")
    public MessageConverterContextFactory<PublicationEntry>
            publicationMessageConverterContextFactory() {
        MessageConverterContextFactory<PublicationEntry> contextFactory =
                new MessageConverterContextFactory<>();

        MessageConverterContext<PublicationEntry> jsonContext =
                MessageConverterContext.<PublicationEntry>builder()
                        .resource(MessageConverterContextFactory.Resource.UNIPROT_PUBLICATION)
                        .contentType(APPLICATION_JSON)
                        .build();
        contextFactory.addMessageConverterContext(jsonContext);

        return contextFactory;
    }

    private MessageConverterContext<UniProtKBEntry> context(MediaType contentType) {
        return MessageConverterContext.<UniProtKBEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPROT)
                .contentType(contentType)
                .build();
    }
}
