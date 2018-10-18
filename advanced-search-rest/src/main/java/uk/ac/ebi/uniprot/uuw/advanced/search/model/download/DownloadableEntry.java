package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;

public class DownloadableEntry implements Downloadable {
    private final UPEntry entry;
    private final List<String> fields;


    public static final List<String> FIELDS = Arrays.asList("accession", "id", "score", "protein_existence");

    public static final String FIELD_FEATURE = "feature";

    public DownloadableEntry(UPEntry entry, List<String> fields) {
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
        if (DownloadableComments.contains(fields)) {
            addData(map, new DownloadableComments(entry.getComments()));
        }
        if (DownloadableEncoded.contains(fields)) {
            addData(map, new DownloadableEncoded(entry.getGeneLocations()));
        }
        if (DownloadableDbXRef.contains(fields)) {
            addData(map, new DownloadableDbXRef(entry.getDbReferences()));
        }

        if (DownloadableEntryInfo.contains(fields)) {
            addData(map, new DownloadableEntryInfo(entry.getEntryInfo()));
        }
        if (DownloadableFeatures.contains(fields)) {
            addData(map, new DownloadableFeatures(entry.getFeatures()));
        }
        if (DownloadableFeatures.contains(fields)) {
            addData(map, new DownloadableFeatures(entry.getFeatures()));
        }
        if (DownloadableGene.contains(fields)) {
            addData(map, new DownloadableGene(entry.getGene()));
        }
        if (DownloadableKeyword.contains(fields)) {
            addData(map, new DownloadableKeyword(entry.getKeywords()));
        }
        if (DownloadableLineage.contains(fields)) {
            addData(map, new DownloadableLineage(entry.getLineage()));
        }

        if (DownloadableOrganism.contains(fields)) {
            addData(map, new DownloadableOrganism(entry.getOrganism()));
        }
        if (DownloadableOrganism.contains(fields)) {
            addData(map, new DownloadableOrganism(entry.getOrganism()));
        }

        if (DownloadableOrganismHost.contains(fields)) {
            addData(map, new DownloadableOrganismHost(entry.getOrganismHost()));
        }
        if (DownloadableProtein.contains(fields)) {
            addData(map, new DownloadableProtein(entry.getProtein()));
        }
        if (DownloadableProtein.contains(fields)) {
            addData(map, new DownloadableProtein(entry.getProtein()));
        }
        if (DownloadableReference.contains(fields)) {
            addData(map, new DownloadableReference(entry.getReferences()));
        }
        if (DownloadableSequence.contains(fields)) {
            addData(map, new DownloadableSequence(entry.getSequence()));
        }
        if (contains(fields)) {
            map.putAll(getSimpleFields());
        }
        if (fields.contains(FIELD_FEATURE)) {
            map.put(FIELD_FEATURE, getFeatures());
        }
        return map;
    }

    private void addData(Map<String, String> map, Downloadable dl) {
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
        listFeatures.addAll(DownloadableFeatures.getFeatures(entry.getFeatures()));
        listFeatures.addAll(DownloadableComments.getSequenceCautionTypes(entry.getComments()));

        if (!listFeatures.isEmpty()) {
            return String.join("; ", listFeatures);
        } else
            return "";
    }
}
