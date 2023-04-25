package org.uniprot.api.uniref.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.api.uniref.output.converter.UniRefFastaMessageConverter;
import org.uniprot.api.uniref.output.converter.UniRefLightFastaMessageConverter;
import org.uniprot.api.uniref.output.converter.UniRefXmlMessageConverter;
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

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
@Configuration
@Getter
@Setter
public class UniRefMessageConverterConfig {
    @Bean
    public WebMvcConfigurer extendedMessageConverters(Gatekeeper downloadGatekeeper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                ReturnFieldConfig returnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF);

                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXlsConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(new ListMessageConverter(downloadGatekeeper));
                converters.add(new UniRefLightFastaMessageConverter(downloadGatekeeper));
                converters.add(new UniRefFastaMessageConverter(downloadGatekeeper));
                converters.add(new RDFMessageConverter(downloadGatekeeper));
                converters.add(new TTLMessageConverter(downloadGatekeeper));
                converters.add(new NTMessageConverter(downloadGatekeeper));
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
                        uniRefLightContext(LIST_MEDIA_TYPE),
                        uniRefLightContext(APPLICATION_XML),
                        uniRefLightContext(APPLICATION_JSON),
                        uniRefLightContext(FASTA_MEDIA_TYPE),
                        uniRefLightContext(TSV_MEDIA_TYPE),
                        uniRefLightContext(XLS_MEDIA_TYPE),
                        uniRefLightContext(RDF_MEDIA_TYPE),
                        uniRefLightContext(TTL_MEDIA_TYPE),
                        uniRefLightContext(NT_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public MessageConverterContextFactory<UniRefMember>
            uniRefMemberMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefMember> contextFactory =
                new MessageConverterContextFactory<>();

        asList(uniRefMemberContext(LIST_MEDIA_TYPE), uniRefMemberContext(APPLICATION_JSON))
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
                        uniRefContext(XLS_MEDIA_TYPE),
                        uniRefContext(RDF_MEDIA_TYPE),
                        uniRefContext(TTL_MEDIA_TYPE),
                        uniRefContext(NT_MEDIA_TYPE))
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
