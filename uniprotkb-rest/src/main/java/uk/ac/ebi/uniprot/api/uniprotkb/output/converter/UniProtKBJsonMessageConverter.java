package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.converter.JsonMessageConverter;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.json.parser.uniprot.UniprotJsonConfig;
import uk.ac.ebi.uniprot.search.field.UniProtField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UniProtKBJsonMessageConverter extends JsonMessageConverter<UniProtEntry> {

    public UniProtKBJsonMessageConverter() {
        super(UniprotJsonConfig.getInstance().getSimpleObjectMapper(), UniProtEntry.class, Arrays.asList(UniProtField.ResultFields.values()));
    }

    @Override
    protected Map<String, List<String>> getFilterFieldMap(String fields) {
        return FieldsParser.parseForFilters(fields);
    }

}
