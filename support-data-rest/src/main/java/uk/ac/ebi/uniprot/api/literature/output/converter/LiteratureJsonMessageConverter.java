package uk.ac.ebi.uniprot.api.literature.output.converter;

import uk.ac.ebi.uniprot.api.literature.output.LiteratureEntryFilter;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.json.parser.literature.LiteratureJsonConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
public class LiteratureJsonMessageConverter extends AbstractJsonMessageConverter<LiteratureEntry> {

    public LiteratureJsonMessageConverter() {
        super(LiteratureJsonConfig.getInstance().getSimpleObjectMapper(), LiteratureEntry.class);
    }

    @Override
    protected LiteratureEntry filterEntryContent(LiteratureEntry entity) {
        Map<String, List<String>> filters = getThreadLocalFilterMap();
        if (Utils.notEmpty(filters)) {
            entity = LiteratureEntryFilter.filterEntry(entity, new ArrayList<>(filters.keySet()));
        }
        return entity;
    }

}