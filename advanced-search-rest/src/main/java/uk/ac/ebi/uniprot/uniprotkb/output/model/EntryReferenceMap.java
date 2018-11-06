package uk.ac.ebi.uniprot.uniprotkb.output.model;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.ReferenceConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.citation.Reference;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class EntryReferenceMap implements NamedValueMap {
    public static final List<String> FIELDS = Arrays.asList("pm_id");
    private final List<Reference> references;

    public EntryReferenceMap(List<Reference> references) {
        if (references == null) {
            this.references = Collections.emptyList();
        } else {
            this.references = Collections.unmodifiableList(references);
        }
    }

    @Override
    public Map<String, String> attributeValues() {
        if (references.isEmpty()) {
            return Collections.emptyMap();
        }

        String result = references.stream().map(Reference::getCitation).filter(val -> val.getDbReferences() != null)
                .flatMap(val -> val.getDbReferences().stream())
                .filter(val -> val.getType().equals(ReferenceConverter.PUB_MED)).map(DbReference::getId)
                .collect(Collectors.joining("; "));
        Map<String, String> map = new HashMap<>();
        map.put(FIELDS.get(0), result);
        return map;
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);

    }
}
