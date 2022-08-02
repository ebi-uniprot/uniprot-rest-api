package org.uniprot.api.common.repository.stream.store.uniprotkb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.util.Utils;

public class UniProtKBBatchStoreIterableUtil {

    public static List<UniProtKBEntry> populateLineageInEntry(
            TaxonomyLineageService taxonomyLineageService, List<UniProtKBEntry> entries) {
        Set<Long> organismIds =
                entries.stream()
                        .map(UniProtKBEntry::getOrganism)
                        .map(Organism::getTaxonId)
                        .collect(Collectors.toSet());
        Map<Long, List<TaxonomyLineage>> lineageMap = taxonomyLineageService.findByIds(organismIds);

        entries =
                entries.stream()
                        .map(entry -> addLineage(entry, lineageMap))
                        .collect(Collectors.toList());
        return entries;
    }

    private static UniProtKBEntry addLineage(
            UniProtKBEntry entry, Map<Long, List<TaxonomyLineage>> lineageMap) {
        List<TaxonomyLineage> lineage = lineageMap.get(entry.getOrganism().getTaxonId());
        if (Utils.notNull(lineage)) {
            UniProtKBEntryBuilder builder = UniProtKBEntryBuilder.from(entry);
            return builder.lineagesSet(lineage).build();
        } else {
            return entry;
        }
    }
}
