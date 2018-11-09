package uk.ac.ebi.uniprot.uniprotkb.output.model;

import com.google.common.base.Strings;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference.Property;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class EntryGoXrefMap implements NamedValueMap {
    private final List<DbReference> dbReferences;
    public static final List<String> FIELDS = Arrays.asList("go", "go_c", "go_f", "go_p", "go_id");

    public EntryGoXrefMap(List<DbReference> dbReferences) {
        if (dbReferences == null) {
            this.dbReferences = Collections.emptyList();
        } else {
            this.dbReferences = Collections.unmodifiableList(dbReferences);
        }

    }

    @Override
    public Map<String, String> attributeValues() {
        if (dbReferences.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        addTypedGoXRefToMap(map, FIELDS.get(0), "", dbReferences);
        addTypedGoXRefToMap(map, FIELDS.get(1), "C", dbReferences);
        addTypedGoXRefToMap(map, FIELDS.get(2), "F", dbReferences);
        addTypedGoXRefToMap(map, FIELDS.get(3), "P", dbReferences);

        map.put(FIELDS.get(4), dbReferences.stream().map(DbReference::getId).sorted()
                .collect(Collectors.joining("; ")));

        return map;
    }

    private void addTypedGoXRefToMap(Map<String, String> map, String field, String type, List<DbReference> xrefs) {
        String result = xrefs.stream().map(val -> getGoTypedString(val, type))
                .filter(val -> !Strings.isNullOrEmpty(val)).collect(Collectors.joining("; "));
        if (!Strings.isNullOrEmpty(result)) {
            map.put(field, result);
        }
    }

    private String getGoTypedString(DbReference xref, String type) {
        Optional<Property> result;
        if (Strings.isNullOrEmpty(type)) {
            result = xref.getProperties().stream().filter(val -> val.getType().equals("term")).findFirst();
        } else
            result = xref.getProperties().stream()
                    .filter(val -> (val.getType().equals("term") && val.getValue().startsWith(type))).findFirst();
        return result
                .map(property -> property.getValue().substring(2) + " [" + xref.getId() + "]")
                .orElse(null);
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);

    }
}
