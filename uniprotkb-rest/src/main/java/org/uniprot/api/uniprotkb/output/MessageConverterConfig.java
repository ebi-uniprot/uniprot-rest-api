package org.uniprot.api.uniprotkb.output;

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
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.output.converter.*;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.interaction.InteractionEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Getter
@Setter
public class MessageConverterConfig {
    /*
     * Add to the supported message converters.
     * Add more message converters for additional response types.
     */
    @Bean
    public WebMvcConfigurer extendedMessageConverters(Gatekeeper downloadGatekeeper) {
        ReturnFieldConfig returnConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        JsonMessageConverter<UniProtKBEntry> uniProtKBJsonMessageConverter =
                new JsonMessageConverter<>(
                        UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniProtKBEntry.class,
                        returnConfig,
                        downloadGatekeeper);
        JsonMessageConverter<InteractionEntry> interactionJsonMessageConverter =
                new JsonMessageConverter<>(
                        UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                        InteractionEntry.class,
                        returnConfig,
                        downloadGatekeeper);

        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                int index = 0;
                converters.add(index++, uniProtKBJsonMessageConverter);
                converters.add(index++, new PublicationJsonMessageConverter(downloadGatekeeper));
                converters.add(index++, interactionJsonMessageConverter);
                converters.add(index++, new UniProtKBFlatFileMessageConverter(downloadGatekeeper));
                converters.add(index++, new UniProtKBFastaMessageConverter(downloadGatekeeper));
                converters.add(index++, new ListMessageConverter(downloadGatekeeper));
                converters.add(index++, new RDFMessageConverter(downloadGatekeeper));
                converters.add(index++, new TTLMessageConverter(downloadGatekeeper));
                converters.add(index++, new NTMessageConverter(downloadGatekeeper));
                converters.add(index++, new UniProtKBGffMessageConverter(downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                UniProtKBEntry.class,
                                returnConfig,
                                new UniProtKBEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                UniProtKBEntry.class,
                                returnConfig,
                                new UniProtKBEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(index++, new ErrorMessageConverter());
                converters.add(index++, new ErrorMessageXlsConverter());
                converters.add(index++, new UniProtKBXmlMessageConverter(downloadGatekeeper));
                converters.add(
                        index++, new ErrorMessageXMLConverter()); // to handle xml error messages

                converters.add(index, new InteractionXmlMessageConverter(downloadGatekeeper));
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
                        context(TTL_MEDIA_TYPE),
                        context(NT_MEDIA_TYPE),
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
                        .resource(MessageConverterContextFactory.Resource.UNIPROTKB_PUBLICATION)
                        .contentType(APPLICATION_JSON)
                        .build();
        contextFactory.addMessageConverterContext(jsonContext);

        return contextFactory;
    }

    @Bean("interactionMessageConverterContextFactory")
    public MessageConverterContextFactory<InteractionEntry>
            interactionMessageConverterContextFactory() {
        MessageConverterContextFactory<InteractionEntry> contextFactory =
                new MessageConverterContextFactory<>();

        contextFactory.addMessageConverterContext(
                MessageConverterContext.<InteractionEntry>builder()
                        .resource(MessageConverterContextFactory.Resource.UNIPROTKB_INTERACTION)
                        .contentType(APPLICATION_JSON)
                        .build());
        contextFactory.addMessageConverterContext(
                MessageConverterContext.<InteractionEntry>builder()
                        .resource(MessageConverterContextFactory.Resource.UNIPROTKB_INTERACTION)
                        .contentType(APPLICATION_XML)
                        .build());

        return contextFactory;
    }

    private MessageConverterContext<UniProtKBEntry> context(MediaType contentType) {
        return MessageConverterContext.<UniProtKBEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPROTKB)
                .contentType(contentType)
                .build();
    }
}
