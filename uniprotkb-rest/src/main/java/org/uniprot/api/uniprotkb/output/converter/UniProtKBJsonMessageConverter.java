package org.uniprot.api.uniprotkb.output.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.uniprotkb.controller.request.FieldsParser;
import org.uniprot.core.json.parser.uniprot.UniprotJsonConfig;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.store.search.field.UniProtField;

public class UniProtKBJsonMessageConverter extends JsonMessageConverter<UniProtEntry> {

    public UniProtKBJsonMessageConverter() {
        super(
                UniprotJsonConfig.getInstance().getSimpleObjectMapper(),
                UniProtEntry.class,
                Arrays.asList(UniProtField.ResultFields.values()));
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        return FieldsParser.parseForFilters(fields);
    }
}
