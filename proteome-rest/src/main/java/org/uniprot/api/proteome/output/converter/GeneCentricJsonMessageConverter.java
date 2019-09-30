package org.uniprot.api.proteome.output.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.search.field.GeneCentricField;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class GeneCentricJsonMessageConverter extends JsonMessageConverter<CanonicalProtein> {

    public GeneCentricJsonMessageConverter() {
        super(
                ProteomeJsonConfig.getInstance().getFullObjectMapper(),
                CanonicalProtein.class,
                Arrays.asList(GeneCentricField.ResultFields.values()));
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        return new HashMap<>();
    }
}
