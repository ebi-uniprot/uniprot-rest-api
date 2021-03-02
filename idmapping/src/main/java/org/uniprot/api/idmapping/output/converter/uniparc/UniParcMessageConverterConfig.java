package org.uniprot.api.idmapping.output.converter.uniparc;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

import java.util.List;

/**
 * @author sahmad
 * @created 02/03/2021
 * Converters related to UniParcEntryPair
 */
public class UniParcMessageConverterConfig {

    public static int appendUniParcConverters(int currentIndex, List<HttpMessageConverter<?>> converters,
                                                                       ReturnFieldConfig returnFieldConfig){
        JsonMessageConverter<UniParcEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniParcEntryPair.class,
                        returnFieldConfig);
        converters.add(currentIndex++, jsonMessageConverter);
        converters.add(currentIndex++, new UniParcEntryPairFastaMessageConverter());
        return currentIndex;
    }

    private UniParcMessageConverterConfig(){}
}
