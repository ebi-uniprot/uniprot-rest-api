package org.uniprot.api.uniparc.common.repository.store.crossref;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.core.CrossReference;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.util.Utils;

import lombok.Getter;

@Component
public class UniParcCrossReferenceFacetConfig extends FacetConfig {
    public static final String ACTIVE_FIELD = "active";
    public static final String DBTYPES_FIELD = "dbTypes";
    public static final String TAXONIDS_FIELD = "taxonIds";

    public static final String ACTIVE_STR = "Active";
    public static final String INACTIVE_STR = "Inactive";

    @Getter
    enum UniParcCrossReferenceFacet {
        STATUS("status", "Status"),
        ORGANISMS("organisms", "Organisms"),
        DATABASES("databases", "Databases");

        private final String label;
        private final String facetName;

        UniParcCrossReferenceFacet(String facetName, String label) {
            this.facetName = facetName;
            this.label = label;
        }
    }

    @Override
    public Collection<String> getFacetNames() {
        return Arrays.stream(UniParcCrossReferenceFacet.values())
                .map(UniParcCrossReferenceFacet::getFacetName)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return null;
    }

    public List<Facet> getUniParcLightFacets(
            Stream<UniParcCrossReference> crossReferences, String commaSeparatedFacetNames) {

        // keep only active, database and organism for facet count
        List<UniParcCrossReference> lightCrossReferences =
                crossReferences.map(this::getLightCrossReference).toList();

        Set<String> facetNames = new HashSet<>();

        if (Utils.notNullNotEmpty(commaSeparatedFacetNames)) {
            facetNames =
                    Arrays.stream(commaSeparatedFacetNames.split(","))
                            .map(String::strip)
                            .collect(Collectors.toSet());
        }

        List<Facet> facets = new ArrayList<>();

        if (facetNames.contains(UniParcCrossReferenceFacet.STATUS.getFacetName())) {
            Facet status =
                    Facet.builder()
                            .label(UniParcCrossReferenceFacet.STATUS.getLabel())
                            .name(ACTIVE_FIELD)
                            .allowMultipleSelection(false)
                            .values(getFacetItemStatuses(lightCrossReferences))
                            .build();
            if (Utils.notNullNotEmpty(status.getValues())) {
                facets.add(status);
            }
        }

        if (facetNames.contains(UniParcCrossReferenceFacet.ORGANISMS.getFacetName())) {
            Facet organisms =
                    Facet.builder()
                            .label(UniParcCrossReferenceFacet.ORGANISMS.getLabel())
                            .name(TAXONIDS_FIELD)
                            .allowMultipleSelection(false)
                            .values(getFacetItemOrganisms(lightCrossReferences))
                            .build();
            if (Utils.notNullNotEmpty(organisms.getValues())) {
                facets.add(organisms);
            }
        }

        if (facetNames.contains(UniParcCrossReferenceFacet.DATABASES.getFacetName())) {
            Facet databases =
                    Facet.builder()
                            .label(UniParcCrossReferenceFacet.DATABASES.getLabel())
                            .name(DBTYPES_FIELD)
                            .allowMultipleSelection(false)
                            .values(getFacetItemDatabases(lightCrossReferences))
                            .build();
            if (Utils.notNullNotEmpty(databases.getValues())) {
                facets.add(databases);
            }
        }

        return facets;
    }

    private UniParcCrossReference getLightCrossReference(UniParcCrossReference crossReference) {
        UniParcCrossReferenceBuilder builder = new UniParcCrossReferenceBuilder();
        builder.active(crossReference.isActive());
        builder.database(crossReference.getDatabase());
        builder.organism(crossReference.getOrganism());
        return builder.build();
    }

    List<FacetItem> getFacetItemStatuses(List<UniParcCrossReference> crossReferences) {
        Map<String, Long> statusCount =
                crossReferences.stream()
                        .collect(
                                Collectors.groupingBy(
                                        crossRef -> crossRef.isActive() ? ACTIVE_STR : INACTIVE_STR,
                                        Collectors.counting()));
        Map<String, String> labelNameMap =
                Map.of(ACTIVE_STR, Boolean.TRUE.toString(), INACTIVE_STR, Boolean.FALSE.toString());
        return statusCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> getStatusFacetItem(entry, labelNameMap))
                .toList();
    }

    List<FacetItem> getFacetItemOrganisms(List<UniParcCrossReference> crossReferences) {
        // taxonId to count map
        Map<Long, Long> taxonIdCount =
                crossReferences.stream()
                        .filter(crossRef -> Objects.nonNull(crossRef.getOrganism()))
                        .collect(
                                Collectors.groupingBy(
                                        crossRef -> crossRef.getOrganism().getTaxonId(),
                                        Collectors.counting()));
        // taxonId to common/scientific name map
        Map<Long, String> taxonIdName =
                crossReferences.stream()
                        .map(UniParcCrossReference::getOrganism)
                        .filter(Objects::nonNull)
                        .filter(
                                organism ->
                                        organism.hasCommonName() || organism.hasScientificName())
                        .collect(
                                Collectors.toMap(
                                        Organism::getTaxonId,
                                        organism ->
                                                organism.hasCommonName()
                                                        ? organism.getCommonName()
                                                        : organism.getScientificName(),
                                        (e1, e2) -> e1));
        return taxonIdCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(entry -> getOrganismFacetItem(entry, taxonIdName))
                .filter(Objects::nonNull)
                .toList();
    }

    List<FacetItem> getFacetItemDatabases(List<UniParcCrossReference> crossReferences) {
        Map<UniParcDatabase, Long> organismCount =
                crossReferences.stream()
                        .filter(CrossReference::hasDatabase)
                        .collect(
                                Collectors.groupingBy(
                                        CrossReference::getDatabase, Collectors.counting()));
        // sort so that swissprot and trembl are first two entries and rest are sorted by count in
        // descending order
        Map<String, Long> orderedOrganismCount =
                organismCount.entrySet().stream()
                        .sorted(this::compareByDatabasePriority)
                        .collect(
                                Collectors.toMap(
                                        organism -> organism.getKey().getDisplayName(),
                                        Map.Entry::getValue,
                                        (e1, e2) -> e1,
                                        LinkedHashMap::new));

        // convert to facet items
        return orderedOrganismCount.entrySet().stream().map(this::getDatabaseFacetItem).toList();
    }

    private int compareByDatabasePriority(
            Map.Entry<UniParcDatabase, Long> entry1, Map.Entry<UniParcDatabase, Long> entry2) {
        UniParcDatabase db1 = entry1.getKey();
        UniParcDatabase db2 = entry2.getKey();

        if (db1 == UniParcDatabase.SWISSPROT) return -1;
        if (db2 == UniParcDatabase.SWISSPROT) return 1;
        if (db1 == UniParcDatabase.TREMBL) return -1;
        if (db2 == UniParcDatabase.TREMBL) return 1;

        return Long.compare(entry2.getValue(), entry1.getValue());
    }

    private FacetItem getOrganismFacetItem(
            Map.Entry<Long, Long> taxonIdCount, Map<Long, String> taxonIdName) {
        if (!taxonIdName.containsKey(taxonIdCount.getKey())) {
            return null;
        }
        String label = taxonIdName.get(taxonIdCount.getKey());
        return FacetItem.builder()
                .label(label)
                .value(String.valueOf(taxonIdCount.getKey()))
                .count(taxonIdCount.getValue())
                .build();
    }

    private FacetItem getStatusFacetItem(
            Map.Entry<String, Long> statusCount, Map<String, String> labelNameMap) {
        String value = labelNameMap.get(statusCount.getKey());
        return FacetItem.builder()
                .label(statusCount.getKey())
                .value(value)
                .count(statusCount.getValue())
                .build();
    }

    private FacetItem getDatabaseFacetItem(Map.Entry<String, Long> dbCount) {
        return FacetItem.builder()
                .label(dbCount.getKey())
                .value(dbCount.getKey())
                .count(dbCount.getValue())
                .build();
    }
}
