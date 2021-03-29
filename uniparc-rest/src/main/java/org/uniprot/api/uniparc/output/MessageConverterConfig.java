package org.uniprot.api.uniparc.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
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
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.ListMessageConverter;
import org.uniprot.api.rest.output.converter.RDFMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.api.rest.output.converter.XlsMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcFastaMessageConverter;
import org.uniprot.api.uniparc.output.converter.UniParcXmlMessageConverter;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryCrossRefValueMapper;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;
import org.uniprot.core.uniparc.UniParcCrossReference;
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
                // ####################### UniParcCrossReference ###################
                ReturnFieldConfig uniParcCrossRefReturnField =
                        ReturnFieldConfigFactory.getReturnFieldConfig(
                                UniProtDataType.UNIPARC_CROSSREF);
                JsonMessageConverter<UniParcCrossReference> uniParcCrossRefJsonConverter =
                        new JsonMessageConverter<>(
                                UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniParcCrossReference.class,
                                uniParcCrossRefReturnField);
                converters.add(2, uniParcCrossRefJsonConverter);
                converters.add(
                        new TsvMessageConverter<>(
                                UniParcCrossReference.class,
                                uniParcCrossRefReturnField,
                                new UniParcEntryCrossRefValueMapper()));
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<UniParcEntry> uniparcMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniParcContext(LIST_MEDIA_TYPE),
                        uniParcContext(APPLICATION_XML),
                        uniParcContext(APPLICATION_JSON),
                        uniParcContext(FASTA_MEDIA_TYPE),
                        uniParcContext(TSV_MEDIA_TYPE),
                        uniParcContext(XLS_MEDIA_TYPE),
                        uniParcContext(RDF_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniParcCrossReference>
            uniParcCrossReferenceMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcCrossReference> contextFactory =
                new MessageConverterContextFactory<>();
        contextFactory.addMessageConverterContext(uniParcCrossRefContext(APPLICATION_JSON));
        contextFactory.addMessageConverterContext(uniParcCrossRefContext(TSV_MEDIA_TYPE));
        return contextFactory;
    }

    private MessageConverterContext<UniParcEntry> uniParcContext(MediaType contentType) {
        return MessageConverterContext.<UniParcEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniParcCrossReference> uniParcCrossRefContext(
            MediaType contentType) {
        return MessageConverterContext.<UniParcCrossReference>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .build();
    }
}
