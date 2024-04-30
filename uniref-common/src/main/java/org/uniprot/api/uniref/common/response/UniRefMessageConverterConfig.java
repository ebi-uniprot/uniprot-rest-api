package org.uniprot.api.uniref.common.response;

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
import org.uniprot.api.uniref.common.response.converter.UniRefFastaMessageConverter;
import org.uniprot.api.uniref.common.response.converter.UniRefLightFastaMessageConverter;
import org.uniprot.api.uniref.common.response.converter.UniRefXmlMessageConverter;
import org.uniprot.core.json.parser.uniref.UniRefEntryJsonConfig;
import org.uniprot.core.json.parser.uniref.UniRefEntryLightJsonConfig;
import org.uniprot.core.json.parser.uniref.UniRefMemberJsonConfig;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryLightValueMapper;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryValueMapper;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefMember;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
@Configuration
@Getter
@Setter
public class UniRefMessageConverterConfig {
    @Bean
    public WebMvcConfigurer uniRefExtendedMessageConverters(Gatekeeper downloadGatekeeper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                ReturnFieldConfig returnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF);

                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXlsConverter());
                converters.add(new ErrorMessageTurtleConverter());
                converters.add(new ErrorMessageNTriplesConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(new ListMessageConverter(downloadGatekeeper));
                converters.add(new UniRefLightFastaMessageConverter(downloadGatekeeper));
                converters.add(new UniRefFastaMessageConverter(downloadGatekeeper));
                converters.add(new RdfMessageConverter(downloadGatekeeper));
                converters.add(new TurtleMessageConverter(downloadGatekeeper));
                converters.add(new NTriplesMessageConverter(downloadGatekeeper));
                converters.add(
                        new TsvMessageConverter<>(
                                UniRefEntryLight.class,
                                returnConfig,
                                new UniRefEntryLightValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        new TsvMessageConverter<>(
                                UniRefEntry.class,
                                returnConfig,
                                new UniRefEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        new XlsMessageConverter<>(
                                UniRefEntryLight.class,
                                returnConfig,
                                new UniRefEntryLightValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        new XlsMessageConverter<>(
                                UniRefEntry.class,
                                returnConfig,
                                new UniRefEntryValueMapper(),
                                downloadGatekeeper));

                JsonMessageConverter<UniRefEntryLight> unirefLightJsonMessageConverter =
                        new JsonMessageConverter<>(
                                UniRefEntryLightJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniRefEntryLight.class,
                                returnConfig,
                                downloadGatekeeper);
                converters.add(0, unirefLightJsonMessageConverter);

                JsonMessageConverter<UniRefEntry> unirefJsonMessageConverter =
                        new JsonMessageConverter<>(
                                UniRefEntryJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniRefEntry.class,
                                returnConfig,
                                downloadGatekeeper);
                converters.add(1, unirefJsonMessageConverter);

                JsonMessageConverter<UniRefMember> unirefMemberJsonMessageConverter =
                        new JsonMessageConverter<>(
                                UniRefMemberJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniRefMember.class,
                                returnConfig,
                                downloadGatekeeper);
                converters.add(2, unirefMemberJsonMessageConverter);

                converters.add(3, new UniRefXmlMessageConverter("", "", downloadGatekeeper));
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<UniRefEntryLight>
            uniRefLightMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefEntryLight> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniRefLightContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniRefLightContext(MediaType.APPLICATION_XML),
                        uniRefLightContext(MediaType.APPLICATION_JSON),
                        uniRefLightContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        uniRefLightContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniRefLightContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        uniRefLightContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        uniRefLightContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        uniRefLightContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniRefMember>
            uniRefMemberMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefMember> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniRefMemberContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniRefMemberContext(MediaType.APPLICATION_JSON))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniRefEntry> uniRefMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniRefContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniRefContext(MediaType.APPLICATION_XML),
                        uniRefContext(MediaType.APPLICATION_JSON),
                        uniRefContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
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

    private MessageConverterContext<UniRefMember> uniRefMemberContext(MediaType contentType) {
        return MessageConverterContext.<UniRefMember>builder()
                .resource(MessageConverterContextFactory.Resource.UNIREF)
                .contentType(contentType)
                .build();
    }
}
