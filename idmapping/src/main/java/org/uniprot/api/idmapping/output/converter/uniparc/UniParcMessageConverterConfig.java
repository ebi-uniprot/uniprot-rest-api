package org.uniprot.api.idmapping.output.converter.uniparc;

import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_FOOTER;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_HEADER;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.output.converter.EntryPairValueMapper;
import org.uniprot.api.idmapping.output.converter.EntryPairXmlMessageConverter;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.ListMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.api.rest.output.converter.XlsMessageConverter;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

/**
 * @author sahmad
 * @created 02/03/2021 Converters related to UniParcEntryPair
 */
public class UniParcMessageConverterConfig {

    public static int appendUniParcConverters(
            int currentIndex,
            List<HttpMessageConverter<?>> converters,
            ReturnFieldConfig returnFieldConfig) {
        JsonMessageConverter<UniParcEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniParcEntryPair.class,
                        returnFieldConfig);
        converters.add(currentIndex++, jsonMessageConverter);
        converters.add(currentIndex++, new UniParcEntryPairFastaMessageConverter());
        converters.add(currentIndex++, new ListMessageConverter());
        converters.add(
                currentIndex++,
                new TsvMessageConverter<>(
                        UniParcEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniParcEntryValueMapper())));
        converters.add(
                currentIndex++,
                new XlsMessageConverter<>(
                        UniParcEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniParcEntryValueMapper())));
        EntryPairXmlMessageConverter<UniParcEntryPair, UniParcEntry, Entry> xmlConverter =
                new EntryPairXmlMessageConverter<>(
                        UniParcEntryPair.class,
                        UNIPARC_XML_CONTEXT,
                        new UniParcEntryConverter(),
                        UNIPARC_XML_HEADER,
                        UNIPARC_XML_FOOTER);
        converters.add(currentIndex++, xmlConverter);

        return currentIndex;
    }

    private UniParcMessageConverterConfig() {}
}
