package org.uniprot.api.idmapping.output.converter.uniref;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.output.converter.EntryPairValueMapper;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.core.json.parser.uniref.UniRefEntryLightJsonConfig;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryLightValueMapper;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

/**
 * @author sahmad
 * @created 02/03/2021
 *     <p>Converters related to UniRefEntryPair
 */
public class UniRefMessageConverterConfig {

    public static int appendUniRefConverters(
            int currentIndex,
            List<HttpMessageConverter<?>> converters,
            ReturnFieldConfig returnFieldConfig,
            Gatekeeper downloadGatekeeper) {
        JsonMessageConverter<UniRefEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniRefEntryLightJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniRefEntryPair.class,
                        returnFieldConfig,
                        downloadGatekeeper);
        converters.add(currentIndex++, jsonMessageConverter);
        converters.add(currentIndex++, new UniRefEntryFastaMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new ListMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new RDFMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new TurtleMessageConverter(downloadGatekeeper));
        converters.add(currentIndex++, new NTriplesMessageConverter(downloadGatekeeper));
        converters.add(
                currentIndex++,
                new TsvMessageConverter<>(
                        UniRefEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniRefEntryLightValueMapper()),
                        downloadGatekeeper));
        converters.add(
                currentIndex++,
                new XlsMessageConverter<>(
                        UniRefEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniRefEntryLightValueMapper()),
                        downloadGatekeeper));
        return currentIndex;
    }

    private UniRefMessageConverterConfig() {}
}
