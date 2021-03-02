package org.uniprot.api.idmapping.output.converter.uniref;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.core.json.parser.uniref.UniRefEntryLightJsonConfig;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

import java.util.List;

/**
 * @author sahmad
 * @created 02/03/2021
 *
 * Converters related to UniRefEntryPair
 */
public class UniRefMessageConverterConfig {

    public static int appendUniRefConverters(int currentIndex, List<HttpMessageConverter<?>> converters,
                                                                       ReturnFieldConfig returnFieldConfig){
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
                        new UniRefEntryPairValueMapper()));
        return currentIndex;
    }

    private UniRefMessageConverterConfig(){}
}
