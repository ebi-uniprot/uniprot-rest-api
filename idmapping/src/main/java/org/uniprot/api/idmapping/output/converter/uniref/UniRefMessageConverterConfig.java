package org.uniprot.api.idmapping.output.converter.uniref;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.output.converter.EntryPairValueMapper;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.api.rest.output.converter.XlsMessageConverter;
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
            ReturnFieldConfig returnFieldConfig) {
        JsonMessageConverter<UniRefEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniRefEntryLightJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniRefEntryPair.class,
                        returnFieldConfig);
        converters.add(currentIndex++, jsonMessageConverter);
        converters.add(currentIndex++, new UniRefEntryFastaMessageConverter());
        converters.add(
                currentIndex++,
                new TsvMessageConverter<>(
                        UniRefEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniRefEntryLightValueMapper())));
        converters.add(
                currentIndex++,
                new XlsMessageConverter<>(
                        UniRefEntryPair.class,
                        returnFieldConfig,
                        new EntryPairValueMapper<>(new UniRefEntryLightValueMapper())));
        return currentIndex;
    }

    private UniRefMessageConverterConfig() {}
}
