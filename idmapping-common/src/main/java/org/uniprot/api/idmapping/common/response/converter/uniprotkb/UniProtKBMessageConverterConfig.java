package org.uniprot.api.idmapping.common.response.converter.uniprotkb;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.EntryPairValueMapper;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

/**
 * @author sahmad
 * @created 02/03/2021 Converters related to UniProtKBEntryPair
 */
public class UniProtKBMessageConverterConfig {

    public static int appendUniProtKBConverters(
            int currentIdx,
            List<HttpMessageConverter<?>> converters,
            ReturnFieldConfig returnFieldConfig,
            Gatekeeper downloadGatekeeper) {
        JsonMessageConverter<UniProtKBEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        downloadGatekeeper);
        converters.add(currentIdx++, jsonMessageConverter);
        converters.add(
                currentIdx++, new UniProtKBEntryPairFlatFileMessageConverter(downloadGatekeeper));
        converters.add(
                currentIdx++, new UniProtKBEntryPairFastaMessageConverter(downloadGatekeeper));
        converters.add(currentIdx++, new ListMessageConverter(downloadGatekeeper));
        converters.add(currentIdx++, new RdfMessageConverter(downloadGatekeeper));
        converters.add(currentIdx++, new TurtleMessageConverter(downloadGatekeeper));
        converters.add(currentIdx++, new NTriplesMessageConverter(downloadGatekeeper));
        converters.add(currentIdx++, new UniProtKBEntryPairGFFMessageConverter(downloadGatekeeper));
        converters.add(
                currentIdx++,
                new TsvMessageConverter<>(
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniProtKBEntryValueMapper()),
                        downloadGatekeeper));
        converters.add(
                currentIdx++,
                new XlsMessageConverter<>(
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniProtKBEntryValueMapper()),
                        downloadGatekeeper));

        String header =
                ConverterConstants.XML_DECLARATION + ConverterConstants.UNIPROTKB_XML_SCHEMA;
        String footer =
                ConverterConstants.COPYRIGHT_TAG + ConverterConstants.UNIPROTKB_XML_CLOSE_TAG;
        UniProtKBEntryPairXmlMessageConverter xmlConverter =
                new UniProtKBEntryPairXmlMessageConverter(
                        UniProtKBEntryPair.class,
                        ConverterConstants.UNIPROTKB_XML_CONTEXT,
                        header,
                        footer,
                        downloadGatekeeper);
        converters.add(currentIdx++, xmlConverter);
        return currentIdx;
    }

    private UniProtKBMessageConverterConfig() {}
}
