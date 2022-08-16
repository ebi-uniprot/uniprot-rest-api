package org.uniprot.api.idmapping.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.uniprot.api.idmapping.output.converter.uniparc.UniParcMessageConverterConfig.appendUniParcConverters;
import static org.uniprot.api.idmapping.output.converter.uniprotkb.UniProtKBMessageConverterConfig.appendUniProtKBConverters;
import static org.uniprot.api.idmapping.output.converter.uniref.UniRefMessageConverterConfig.appendUniRefConverters;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.GFF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;
import static org.uniprot.store.config.UniProtDataType.PIR_ID_MAPPING;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.SerializationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created 21/08/18
 *
 * @author Edd
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
                converters.add(
                        index++, new ErrorMessageXMLConverter()); // to handle xml error messages

                // ------------------------- UniProtKb converters -------------------------

                ReturnFieldConfig uniProtKBReturnFieldCfg =
                        getIdMappingReturnFieldConfig(UniProtDataType.UNIPROTKB);

                index =
                        appendUniProtKBConverters(
                                index, converters, uniProtKBReturnFieldCfg, downloadGatekeeper);

                // ------------------------- UniParcEntryPair -------------------------

                ReturnFieldConfig uniParcReturnFieldCfg =
                        getIdMappingReturnFieldConfig(UniProtDataType.UNIPARC);

                index =
                        appendUniParcConverters(
                                index, converters, uniParcReturnFieldCfg, downloadGatekeeper);

                // ------------------------- UniRefEntryPair -------------------------

                ReturnFieldConfig uniRefReturnFieldCfg =
                        getIdMappingReturnFieldConfig(UniProtDataType.UNIREF);

                index =
                        appendUniRefConverters(
                                index, converters, uniRefReturnFieldCfg, downloadGatekeeper);

                // ------------------------- IdMappingStringPair -------------------------
                JsonMessageConverter<IdMappingStringPair> idMappingPairJsonMessageConverter =
                        new JsonMessageConverter<>(
                                new ObjectMapper(),
                                IdMappingStringPair.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(PIR_ID_MAPPING),
                                downloadGatekeeper);
                converters.add(index++, idMappingPairJsonMessageConverter);
                converters.add(index++, new ListMessageConverter(downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                IdMappingStringPair.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(PIR_ID_MAPPING),
                                new IdMappingStringPairTSVMapper(),
                                downloadGatekeeper));
                converters.add(
                        index,
                        new XlsMessageConverter<>(
                                IdMappingStringPair.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(PIR_ID_MAPPING),
                                new IdMappingStringPairTSVMapper(),
                                downloadGatekeeper));
            }
        };
    }

    @Bean("stringPairMessageConverterContextFactory")
    public MessageConverterContextFactory<IdMappingStringPair>
            stringPairMessageConverterContextFactory() {
        MessageConverterContextFactory<IdMappingStringPair> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        idMappingContext(APPLICATION_JSON),
                        idMappingContext(LIST_MEDIA_TYPE),
                        idMappingContext(TSV_MEDIA_TYPE),
                        idMappingContext(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("stringUniProtKBEntryPairMessageConverterContextFactory")
    public MessageConverterContextFactory<UniProtKBEntryPair>
            stringUniProtKBEntryPairMessageConverterContextFactory() {
        MessageConverterContextFactory<UniProtKBEntryPair> contextFactory =
                new MessageConverterContextFactory<>();

        List.of(
                        kbContext(APPLICATION_JSON),
                        kbContext(FASTA_MEDIA_TYPE),
                        kbContext(TSV_MEDIA_TYPE),
                        kbContext(XLS_MEDIA_TYPE),
                        kbContext(APPLICATION_XML),
                        kbContext(FF_MEDIA_TYPE),
                        kbContext(GFF_MEDIA_TYPE),
                        kbContext(LIST_MEDIA_TYPE),
                        kbContext(RDF_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("uniParcEntryPairMessageConverterContextFactory")
    public MessageConverterContextFactory<UniParcEntryPair>
            uniParcEntryPairMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcEntryPair> contextFactory =
                new MessageConverterContextFactory<>();

        List.of(
                        uniParcContext(APPLICATION_JSON),
                        uniParcContext(FASTA_MEDIA_TYPE),
                        uniParcContext(TSV_MEDIA_TYPE),
                        uniParcContext(XLS_MEDIA_TYPE),
                        uniParcContext(APPLICATION_XML),
                        uniParcContext(LIST_MEDIA_TYPE),
                        uniParcContext(RDF_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("uniRefEntryPairMessageConverterContextFactory")
    public MessageConverterContextFactory<UniRefEntryPair>
            uniRefEntryPairMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefEntryPair> contextFactory =
                new MessageConverterContextFactory<>();

        List.of(
                        uniRefContext(APPLICATION_JSON),
                        uniRefContext(FASTA_MEDIA_TYPE),
                        uniRefContext(TSV_MEDIA_TYPE),
                        uniRefContext(XLS_MEDIA_TYPE),
                        uniRefContext(LIST_MEDIA_TYPE),
                        uniRefContext(RDF_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniParcEntryPair> uniParcContext(MediaType contentType) {
        return MessageConverterContext.<UniParcEntryPair>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPARC)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniRefEntryPair> uniRefContext(MediaType contentType) {
        return MessageConverterContext.<UniRefEntryPair>builder()
                .resource(MessageConverterContextFactory.Resource.UNIREF)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<IdMappingStringPair> idMappingContext(MediaType contentType) {
        return MessageConverterContext.<IdMappingStringPair>builder()
                .resource(MessageConverterContextFactory.Resource.IDMAPPING_PIR)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniProtKBEntryPair> kbContext(MediaType contentType) {
        return MessageConverterContext.<UniProtKBEntryPair>builder()
                .resource(MessageConverterContextFactory.Resource.UNIPROTKB)
                .contentType(contentType)
                .build();
    }

    private ReturnFieldConfig getIdMappingReturnFieldConfig(UniProtDataType dataType) {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(dataType);
        // clone it to avoid messing with the global constant
        ReturnFieldConfig idMappingReturnConfig = SerializationUtils.clone(returnFieldConfig);
        List<ReturnField> returnFields =
                idMappingReturnConfig.getReturnFields().stream()
                        .map(this::updatePath)
                        .collect(Collectors.toList());
        idMappingReturnConfig.getReturnFields().clear();
        ReturnField fromField = getFromReturnField();
        idMappingReturnConfig
                .getReturnFields()
                .add(fromField); // add required from field on the fly
        idMappingReturnConfig.getReturnFields().addAll(returnFields);
        return idMappingReturnConfig;
    }
    // prefix to. in the return field path
    private ReturnField updatePath(ReturnField returnField) {
        List<String> oldPaths =
                returnField.getPaths().stream()
                        .map(path -> "to." + path)
                        .collect(Collectors.toList());
        returnField.setPaths(oldPaths);
        return returnField;
    }

    // add a required field from to be returned in the response all the time
    private ReturnField getFromReturnField() {
        ReturnField fromReturnField = new ReturnField();
        fromReturnField.setName("from");
        fromReturnField.setId("from");
        fromReturnField.addPath("from");
        fromReturnField.setIsRequiredForJson(true);
        fromReturnField.setIsDefaultForTsv(true);
        fromReturnField.setDefaultForTsvOrder(-1);
        fromReturnField.setLabel("From");
        return fromReturnField;
    }
}
