package org.uniprot.api.idmapping.output.converter.uniprotkb;

import static org.uniprot.api.rest.output.converter.ConverterConstants.COPYRIGHT_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CLOSE_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.XML_DECLARATION;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.output.converter.EntryPairValueMapper;
import org.uniprot.api.idmapping.output.converter.EntryPairXmlMessageConverter;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.ListMessageConverter;
import org.uniprot.api.rest.output.converter.RDFMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.api.rest.output.converter.XlsMessageConverter;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.core.xml.uniprot.UniProtEntryConverter;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

/**
 * @author sahmad
 * @created 02/03/2021 Converters related to UniProtKBEntryPair
 */
public class UniProtKBMessageConverterConfig {

    public static int appendUniProtKBConverters(
            int currentIdx,
            List<HttpMessageConverter<?>> converters,
            ReturnFieldConfig returnFieldConfig) {
        JsonMessageConverter<UniProtKBEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniProtKBEntryPair.class,
                        returnFieldConfig);
        converters.add(currentIdx++, jsonMessageConverter);
        converters.add(currentIdx++, new UniProtKBEntryPairFlatFileMessageConverter());
        converters.add(currentIdx++, new UniProtKBEntryPairFastaMessageConverter());
        converters.add(currentIdx++, new ListMessageConverter());
        converters.add(currentIdx++, new RDFMessageConverter());
        converters.add(currentIdx++, new UniProtKBEntryPairGFFMessageConverter());
        converters.add(
                currentIdx++,
                new TsvMessageConverter<>(
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniProtKBEntryValueMapper())));
        converters.add(
                currentIdx++,
                new XlsMessageConverter<>(
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniProtKBEntryValueMapper())));

        String header = XML_DECLARATION + UNIPROTKB_XML_SCHEMA;
        String footer = COPYRIGHT_TAG + UNIPROTKB_XML_CLOSE_TAG;
        EntryPairXmlMessageConverter<UniProtKBEntryPair, UniProtKBEntry, Entry> xmlConverter =
                new EntryPairXmlMessageConverter<>(
                        UniProtKBEntryPair.class,
                        UNIPROTKB_XML_CONTEXT,
                        new UniProtEntryConverter(),
                        header,
                        footer);
        converters.add(currentIdx++, xmlConverter);
        return currentIdx;
    }

    private UniProtKBMessageConverterConfig() {}
}
