package org.uniprot.api.idmapping.common.response.converter.uniparc;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.EntryPairValueMapper;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.core.json.parser.uniparc.UniParcEntryLightJsonConfig;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryLightValueMapper;
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
        JsonMessageConverter<UniParcEntryLightPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniParcEntryLightJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniParcEntryLightPair.class,
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
                        UniParcEntryLightPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniParcEntryLightValueMapper()),
                        downloadGatekeeper));
        converters.add(
                currentIndex++,
                new XlsMessageConverter<>(
                        UniParcEntryLightPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniParcEntryLightValueMapper()),
                        downloadGatekeeper));
        return currentIndex;
    }

    private UniParcMessageConverterConfig() {}
}
