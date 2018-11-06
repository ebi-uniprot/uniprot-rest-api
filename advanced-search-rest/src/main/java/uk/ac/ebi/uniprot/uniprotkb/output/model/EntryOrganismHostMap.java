package uk.ac.ebi.uniprot.uniprotkb.output.model;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class EntryOrganismHostMap implements NamedValueMap {
    public static final List<String> FIELDS = Arrays.asList("organism_host");
    private final List<Organism> organismHost;

    public EntryOrganismHostMap(List<Organism> organismHost) {
        if (organismHost == null) {
            this.organismHost = Collections.emptyList();
        } else {
            this.organismHost = Collections.unmodifiableList(organismHost);
        }
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);
    }

    @Override
    public Map<String, String> attributeValues() {
        if (organismHost.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        map.put(FIELDS.get(0), organismHost.stream().map(this::getOrganismName).collect(Collectors.joining("; ")));
        return map;
    }

    private String getOrganismName(Organism organism) {
        StringBuilder sb = new StringBuilder();
        sb.append(EntryMapUtil.convertOrganism(organism));
        sb.append(" [TaxID: ").append("" + organism.getTaxonomy()).append("]");
        return sb.toString();
    }
}
