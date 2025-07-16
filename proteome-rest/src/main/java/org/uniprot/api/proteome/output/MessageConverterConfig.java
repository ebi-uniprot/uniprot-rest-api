package org.uniprot.api.proteome.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
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

import lombok.Getter;
import lombok.Setter;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */
@Configuration
@Getter
@Setter
public class MessageConverterConfig {
    @Bean
    public WebMvcConfigurer extendedMessageConverters(Gatekeeper downloadGatekeeper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                int index = 0;

                converters.add(index++, new ErrorMessageConverter());
                converters.add(index++, new ErrorMessageXlsConverter());
                converters.add(index++, new ErrorMessageTurtleConverter());
                converters.add(index++, new ErrorMessageNTriplesConverter());
                converters.add(index++, new ErrorMessageXMLConverter()); // to handle xml error
                // messages
                converters.add(index++, new ListMessageConverter(downloadGatekeeper));

                ReturnFieldConfig proteomeReturnFieldConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.PROTEOME);
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                ProteomeEntry.class,
                                proteomeReturnFieldConfig,
                                new ProteomeEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                ProteomeEntry.class,
                                proteomeReturnFieldConfig,
                                new ProteomeEntryValueMapper(),
                                downloadGatekeeper));

                JsonMessageConverter<ProteomeEntry> proteomeJsonConverter =
                        new JsonMessageConverter<>(
                                ProteomeJsonConfig.getInstance().getSimpleObjectMapper(),
                                ProteomeEntry.class,
                                proteomeReturnFieldConfig,
                                downloadGatekeeper);
                converters.add(index++, proteomeJsonConverter);
                converters.add(index++, new ProteomeXmlMessageConverter(downloadGatekeeper));
                converters.add(index++, new RdfMessageConverter(downloadGatekeeper));
                converters.add(index++, new TurtleMessageConverter(downloadGatekeeper));
                converters.add(index, new NTriplesMessageConverter(downloadGatekeeper));

                // GeneCentric related converters
                converters.add(index++, new GeneCentricFastaMessageConverter(downloadGatekeeper));
                JsonMessageConverter<GeneCentricEntry> geneCentricJsonConverter =
                        new JsonMessageConverter<>(
                                GeneCentricJsonConfig.getInstance().getSimpleObjectMapper(),
                                GeneCentricEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.GENECENTRIC),
                                downloadGatekeeper);
                converters.add(index++, geneCentricJsonConverter);
                converters.add(index, new GeneCentricXmlMessageConverter(downloadGatekeeper));
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
                        proteomeContext(XLS_MEDIA_TYPE),
                        proteomeContext(RDF_MEDIA_TYPE),
                        proteomeContext(TURTLE_MEDIA_TYPE),
                        proteomeContext(N_TRIPLES_MEDIA_TYPE))
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
