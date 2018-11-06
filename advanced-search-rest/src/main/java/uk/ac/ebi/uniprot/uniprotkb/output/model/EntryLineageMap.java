package uk.ac.ebi.uniprot.uniprotkb.output.model;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.TaxNode;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class EntryLineageMap implements NamedValueMap {
    private final List<TaxNode> lineage;
    public static final List<String> FIELDS =
            Arrays.asList("lineage", "tl:all", "tl:class", "tl:cohort",
                          "tl:family", "tl:forma", "tl:genus", "tl:infraclass", "tl:infraorder", "tl:kingdom", "tl:order",
                          "tl:parvorder", "tl:phylum", "tl:species", "tl:species_group", "tl:species_subgroup", "tl:subclass",
                          "tl:subcohort", "tl:subfamily", "tl:subgenus", "tl:subkingdom", "tl:suborder", "tl:subphylum",
                          "tl:subspecies", "tl:subtribe", "tl:superclass", "tl:superfamily", "tl:superkingdom", "tl:superorder",
                          "tl:superphylum", "tl:tribe", "tl:varietas");

    public EntryLineageMap(List<TaxNode> lineage) {
        if (lineage == null) {
            this.lineage = Collections.emptyList();
        } else {
            this.lineage = Collections.unmodifiableList(lineage);
        }

    }

    @Override
    public Map<String, String> attributeValues() {
        if (lineage.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        String allLineage = getAll();
        map.put("tl:all", allLineage);
        map.put("lineage", allLineage);
        FIELDS.stream().skip(1).forEach(val -> addToMap(val, map));
        return map;
    }

    private void addToMap(String field, Map<String, String> map) {
        Optional<TaxNode> node = getValue(field);
        node.ifPresent(taxNode -> map.put(field, taxNode.getName()));
    }

    private String getAll() {

        return lineage.stream().map(TaxNode::getName).collect(Collectors.joining(", "));
    }

    private Optional<TaxNode> getValue(String field) {
        String type = field.substring(3);
        return lineage.stream().filter(val -> val.getRank().equalsIgnoreCase(type)).findFirst();
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);

    }
}
