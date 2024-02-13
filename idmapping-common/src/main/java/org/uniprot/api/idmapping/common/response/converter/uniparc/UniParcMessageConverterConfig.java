package org.uniprot.api.idmapping.common.response.converter.uniparc;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.EntryPairValueMapper;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

/**
 * @author sahmad
 * @created 02/03/2021 Converters related to UniParcEntryPair
 */
public class UniParcMessageConverterConfig {

    public static int appendUniParcConverters(
            int currentIndex,
            List<HttpMessageConverter<?>> converters,
            ReturnFieldConfig returnFieldConfig,
            Gatekeeper downloadGatekeeper) {
        JsonMessageConverter<UniParcEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniParcEntryPair.class,
                        returnFieldConfig,
                        downloadGatekeeper);
        converters.add(currentIndex++, jsonMessageConverter);
        converters.add(
                currentIndex++, new UniParcEntryPairFastaMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new ListMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new RdfMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new TurtleMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new NTriplesMessageConverter(downloadGatekeeper));
        converters.add(
                currentIndex++,
                new TsvMessageConverter<>(
                        UniParcEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniParcEntryValueMapper()),
                        downloadGatekeeper));
        converters.add(
                currentIndex++,
                new XlsMessageConverter<>(
                        UniParcEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniParcEntryValueMapper()),
                        downloadGatekeeper));
        String header = ConverterConstants.XML_DECLARATION + ConverterConstants.UNIPARC_XML_SCHEMA;
        String footer = ConverterConstants.COPYRIGHT_TAG + ConverterConstants.UNIPARC_XML_CLOSE_TAG;
        UniParcEntryPairXmlMessageConverter xmlConverter =
                new UniParcEntryPairXmlMessageConverter(
                        UniParcEntryPair.class,
                        ConverterConstants.UNIPARC_XML_CONTEXT,
                        header,
                        footer,
                        downloadGatekeeper);
        converters.add(currentIndex++, xmlConverter);

        return currentIndex;
    }

    private UniParcMessageConverterConfig() {}
}
