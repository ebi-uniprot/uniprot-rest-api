package uk.ac.ebi.uniprot.uniprotkb.output.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

public class EntryMap implements NamedValueMap {
    private final UPEntry entry;
    private final List<String> fields;


    public static final List<String> FIELDS = Arrays.asList("accession", "id", "score", "protein_existence");

    public static final String FIELD_FEATURE = "feature";

    public EntryMap(UPEntry entry, List<String> fields) {
        this.entry = entry;
        this.fields = Collections.unmodifiableList(fields);
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);

    }

    public List<String> getData() {
        List<String> result = new ArrayList<>();
        Map<String, String> mapped = attributeValues();
        for (String field : fields) {
            result.add(mapped.getOrDefault(field, ""));
        }
        return result;
    }

    @Override
    public Map<String, String> attributeValues() {
        Map<String, String> map = new HashMap<>();
        if (EntryCommentsMap.contains(fields)) {
            addData(map, new EntryCommentsMap(entry.getComments()));
        }
        if (EntryEncodedMap.contains(fields)) {
            addData(map, new EntryEncodedMap(entry.getGeneLocations()));
        }
        if (EntryDbXRefMap.contains(fields)) {
            addData(map, new EntryDbXRefMap(entry.getDbReferences()));
        }

        if (EntryInfoMap.contains(fields)) {
            addData(map, new EntryInfoMap(entry.getEntryInfo()));
        }
        if (EntryFeaturesMap.contains(fields)) {
            addData(map, new EntryFeaturesMap(entry.getFeatures()));
        }
        if (EntryFeaturesMap.contains(fields)) {
            addData(map, new EntryFeaturesMap(entry.getFeatures()));
        }
        if (EntryGeneMap.contains(fields)) {
            addData(map, new EntryGeneMap(entry.getGene()));
        }
        if (EntryKeywordMap.contains(fields)) {
            addData(map, new EntryKeywordMap(entry.getKeywords()));
        }
        if (EntryLineageMap.contains(fields)) {
            addData(map, new EntryLineageMap(entry.getLineage()));
        }

        if (EntryOrganismMap.contains(fields)) {
            addData(map, new EntryOrganismMap(entry.getOrganism()));
        }
        if (EntryOrganismMap.contains(fields)) {
            addData(map, new EntryOrganismMap(entry.getOrganism()));
        }

        if (EntryOrganismHostMap.contains(fields)) {
            addData(map, new EntryOrganismHostMap(entry.getOrganismHost()));
        }
        if (EntryProteinMap.contains(fields)) {
            addData(map, new EntryProteinMap(entry.getProtein()));
        }
        if (EntryProteinMap.contains(fields)) {
            addData(map, new EntryProteinMap(entry.getProtein()));
        }
        if (EntryReferenceMap.contains(fields)) {
            addData(map, new EntryReferenceMap(entry.getReferences()));
        }
        if (EntrySequenceMap.contains(fields)) {
            addData(map, new EntrySequenceMap(entry.getSequence()));
        }
        if (contains(fields)) {
            map.putAll(getSimpleFields());
        }
        if (fields.contains(FIELD_FEATURE)) {
            map.put(FIELD_FEATURE, getFeatures());
        }
        return map;
    }

    private void addData(Map<String, String> map, NamedValueMap dl) {
        map.putAll(dl.attributeValues());
    }

    private Map<String, String> getSimpleFields() {
        Map<String, String> map = new HashMap<>();
        map.put("accession", entry.getAccession());
        map.put("id", entry.getId());
        map.put("score", entry.getAnnotationScore() + "");
        if (!Strings.isNullOrEmpty(entry.getProteinExistence())) {
            map.put("protein_existence", entry.getProteinExistence());
        }
        return map;
    }

    private String getFeatures() {
        Set<String> listFeatures = new TreeSet<>();
        listFeatures.addAll(EntryFeaturesMap.getFeatures(entry.getFeatures()));
        listFeatures.addAll(EntryCommentsMap.getSequenceCautionTypes(entry.getComments()));

        if (!listFeatures.isEmpty()) {
            return String.join("; ", listFeatures);
        } else
            return "";
    }
}
