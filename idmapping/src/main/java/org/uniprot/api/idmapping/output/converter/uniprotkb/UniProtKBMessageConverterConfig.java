package org.uniprot.api.idmapping.output.converter.uniprotkb;

import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.api.rest.output.converter.XlsMessageConverter;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

import java.util.List;

/**
 * @author sahmad
 * @created 02/03/2021
 * Converters related to UniProtKBEntryPair
 */
public class UniProtKBMessageConverterConfig {

    public static int appendUniProtKBConverters(int currentIdx, List<HttpMessageConverter<?>> converters,
                                                                       ReturnFieldConfig returnFieldConfig){
        JsonMessageConverter<UniProtKBEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniProtKBEntryPair.class,
                        returnFieldConfig);
        converters.add(currentIdx++, jsonMessageConverter);
        converters.add(currentIdx++, new UniProtKBEntryPairFastaMessageConverter());
        converters.add(
                currentIdx++,
                new TsvMessageConverter<>(
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        new UniProtKBEntryPairValueMapper()));
        converters.add(
                currentIdx++,
                new XlsMessageConverter<>(
                        UniProtKBEntryPair.class,
                        returnFieldConfig,
                        new UniProtKBEntryPairValueMapper()));
        return currentIdx;
    }

    private UniProtKBMessageConverterConfig(){}
}
