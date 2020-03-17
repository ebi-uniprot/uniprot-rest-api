package org.uniprot.api.uniprotkb.output.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.uniprotkb.controller.request.FieldsParser;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.search.field.UniProtField;

public class UniProtKBJsonMessageConverter extends JsonMessageConverter<UniProtKBEntry> {

    public UniProtKBJsonMessageConverter() {
        super(
                UniprotKBJsonConfig.getInstance().getSimpleObjectMapper(),
                UniProtKBEntry.class,
                Arrays.asList(UniProtField.ResultFields.values()));
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        return FieldsParser.parseForFilters(fields);
    }
}
