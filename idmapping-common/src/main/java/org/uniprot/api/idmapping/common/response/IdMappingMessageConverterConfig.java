package org.uniprot.api.idmapping.common.response;

import static java.util.Arrays.asList;
import static org.uniprot.api.idmapping.common.response.converter.uniprotkb.UniProtKBMessageConverterConfig.appendUniProtKBConverters;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.uniparc.UniParcMessageConverterConfig;
import org.uniprot.api.idmapping.common.response.converter.uniref.UniRefMessageConverterConfig;
import org.uniprot.api.idmapping.common.response.model.*;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Getter
@Setter
public class IdMappingMessageConverterConfig {
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
                        UniParcMessageConverterConfig.appendUniParcConverters(
                                index, converters, uniParcReturnFieldCfg, downloadGatekeeper);

                // ------------------------- UniRefEntryPair -------------------------

                ReturnFieldConfig uniRefReturnFieldCfg =
                        getIdMappingReturnFieldConfig(UniProtDataType.UNIREF);

                index =
                        UniRefMessageConverterConfig.appendUniRefConverters(
                                index, converters, uniRefReturnFieldCfg, downloadGatekeeper);

                // ------------------------- IdMappingStringPair -------------------------
                JsonMessageConverter<IdMappingStringPair> idMappingPairJsonMessageConverter =
                        new JsonMessageConverter<>(
                                new ObjectMapper(),
                                IdMappingStringPair.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.PIR_ID_MAPPING),
                                downloadGatekeeper);
                converters.add(index++, idMappingPairJsonMessageConverter);
                converters.add(index++, new ListMessageConverter(downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                IdMappingStringPair.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.PIR_ID_MAPPING),
                                new IdMappingStringPairTSVMapper(),
                                downloadGatekeeper));
                converters.add(
                        index,
                        new XlsMessageConverter<>(
                                IdMappingStringPair.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.PIR_ID_MAPPING),
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
                        idMappingContext(MediaType.APPLICATION_JSON),
                        idMappingContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        idMappingContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        idMappingContext(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("stringUniProtKBEntryPairMessageConverterContextFactory")
    public MessageConverterContextFactory<UniProtKBEntryPair>
            stringUniProtKBEntryPairMessageConverterContextFactory() {
        MessageConverterContextFactory<UniProtKBEntryPair> contextFactory =
                new MessageConverterContextFactory<>();

        List.of(
                        kbContext(MediaType.APPLICATION_JSON),
                        kbContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        kbContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        kbContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        kbContext(MediaType.APPLICATION_XML),
                        kbContext(UniProtMediaType.FF_MEDIA_TYPE),
                        kbContext(UniProtMediaType.GFF_MEDIA_TYPE),
                        kbContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        kbContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        kbContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        kbContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("uniParcEntryLightPairMessageConverterContextFactory")
    public MessageConverterContextFactory<UniParcEntryLightPair>
            uniParcEntryLightPairMessageConverterContextFactory() {
        MessageConverterContextFactory<UniParcEntryLightPair> contextFactory =
                new MessageConverterContextFactory<>();

        List.of(
                        uniParcLightContext(MediaType.APPLICATION_JSON),
                        uniParcLightContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        uniParcLightContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean("uniRefEntryPairMessageConverterContextFactory")
    public MessageConverterContextFactory<UniRefEntryPair>
            uniRefEntryPairMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRefEntryPair> contextFactory =
                new MessageConverterContextFactory<>();

        List.of(
                        uniRefContext(MediaType.APPLICATION_JSON),
                        uniRefContext(UniProtMediaType.FASTA_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.XLS_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.RDF_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        uniRefContext(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniParcEntryLightPair> uniParcLightContext(
            MediaType contentType) {
        return MessageConverterContext.<UniParcEntryLightPair>builder()
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
