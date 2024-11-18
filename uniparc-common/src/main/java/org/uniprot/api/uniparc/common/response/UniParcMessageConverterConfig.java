package org.uniprot.api.uniparc.common.response;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.api.uniparc.common.response.converter.*;
import org.uniprot.core.json.parser.uniparc.UniParcCrossRefJsonConfig;
import org.uniprot.core.json.parser.uniparc.UniParcEntryLightJsonConfig;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryCrossRefValueMapper;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryLightValueMapper;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
@Configuration
@Getter
@Setter
public class UniParcMessageConverterConfig {
    @Bean
    public WebMvcConfigurer uniParcExtendedMessageConverters(Gatekeeper downloadGatekeeper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXlsConverter());
                converters.add(new ErrorMessageTurtleConverter());
                converters.add(new ErrorMessageNTriplesConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(new ListMessageConverter(downloadGatekeeper));

                ReturnFieldConfig returnFieldConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC);

                converters.add(new UniParcFastaMessageConverter(downloadGatekeeper));
                converters.add(
                        new TsvMessageConverter<>(
                                UniParcEntry.class,
                                returnFieldConfig,
                                new UniParcEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        new XlsMessageConverter<>(
                                UniParcEntry.class,
                                returnFieldConfig,
                                new UniParcEntryValueMapper(),
                                downloadGatekeeper));

                JsonMessageConverter<UniParcEntry> uniparcJsonConverter =
                        new JsonMessageConverter<>(
                                UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniParcEntry.class,
                                returnFieldConfig,
                                downloadGatekeeper);
                converters.add(0, uniparcJsonConverter);
                converters.add(1, new UniParcXmlMessageConverter("", downloadGatekeeper));
                converters.add(new RdfMessageConverter(downloadGatekeeper));
                converters.add(new TurtleMessageConverter(downloadGatekeeper));
                converters.add(new NTriplesMessageConverter(downloadGatekeeper));
                // ####################### UniParcCrossReference ###################
                ReturnFieldConfig uniParcCrossRefReturnField =
                        ReturnFieldConfigFactory.getReturnFieldConfig(
                                UniProtDataType.UNIPARC_CROSSREF);
                JsonMessageConverter<UniParcCrossReference> uniParcCrossRefJsonConverter =
                        new JsonMessageConverter<>(
                                UniParcCrossRefJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniParcCrossReference.class,
                                uniParcCrossRefReturnField,
                                downloadGatekeeper);
                converters.add(2, uniParcCrossRefJsonConverter);
                converters.add(3, new UniParcCrossReferenceXMLConverter());
                converters.add(
                        new TsvMessageConverter<>(
                                UniParcCrossReference.class,
                                uniParcCrossRefReturnField,
                                new UniParcEntryCrossRefValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        new XlsMessageConverter<>(
                                UniParcCrossReference.class,
                                uniParcCrossRefReturnField,
                                new UniParcEntryCrossRefValueMapper(),
                                downloadGatekeeper));
                // ####################### UniParcLight ###################
                converters.add(new UniParcLightFastaMessageConverter(downloadGatekeeper));

                converters.add(
                        new TsvMessageConverter<>(
                                UniParcEntryLight.class,
                                returnFieldConfig,
                                new UniParcEntryLightValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        new XlsMessageConverter<>(
                                UniParcEntryLight.class,
                                returnFieldConfig,
                                new UniParcEntryLightValueMapper(),
                                downloadGatekeeper));

                JsonMessageConverter<UniParcEntryLight> uniparcLightJsonConverter =
                        new JsonMessageConverter<>(
                                UniParcEntryLightJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniParcEntryLight.class,
                                returnFieldConfig,
                                downloadGatekeeper);
                converters.add(4, uniparcLightJsonConverter);
                converters.add(5, new UniParcLightXMLMessageConverter("", downloadGatekeeper));
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<UniParcEntry> uniparcMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcEntry> contextFactory =
                new MessageConverterContextFactory<>();
        List.of(
                        uniParcContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniParcContext(MediaType.APPLICATION_XML),
                        uniParcContext(MediaType.APPLICATION_JSON),
                        uniParcContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        uniParcContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniParcContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        uniParcContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        uniParcContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        uniParcContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);
        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniParcEntryLight>
            uniparcLightMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcEntryLight> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniParcLightContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniParcLightContext(MediaType.APPLICATION_JSON),
                        uniParcLightContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE),
                        uniParcLightContext(MediaType.APPLICATION_XML))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniParcCrossReference>
            uniParcCrossReferenceMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcCrossReference> contextFactory =
                new MessageConverterContextFactory<>();
        contextFactory.addMessageConverterContext(
                uniParcCrossRefContext(MediaType.APPLICATION_JSON));
        contextFactory.addMessageConverterContext(
                uniParcCrossRefContext(UniProtMediaType.TSV_MEDIA_TYPE));
        contextFactory.addMessageConverterContext(
                uniParcCrossRefContext(UniProtMediaType.XLS_MEDIA_TYPE));
        contextFactory.addMessageConverterContext(
                uniParcCrossRefContext(MediaType.APPLICATION_XML));
        return contextFactory;
    }

    private MessageConverterContext<UniParcEntry> uniParcContext(MediaType contentType) {
        return MessageConverterContext.<UniParcEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniParcEntryLight> uniParcLightContext(MediaType contentType) {
        return MessageConverterContext.<UniParcEntryLight>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniParcCrossReference> uniParcCrossRefContext(
            MediaType contentType) {
        return MessageConverterContext.<UniParcCrossReference>builder()
                .resource(MessageConverterContextFactory.Resource.CROSSREF)
                .contentType(contentType)
                .build();
    }
}
