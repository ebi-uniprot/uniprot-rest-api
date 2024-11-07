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

    public List<Facet> getUniParcCrossReferenceFacets(
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
        return statusCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(this::getStatusFacetItem)
                .toList();
    }

    List<FacetItem> getFacetItemOrganisms(List<UniParcCrossReference> crossReferences) {
        // taxon to count map
        Map<Organism, Long> taxonCount =
                crossReferences.stream()
                        .filter(crossRef -> Objects.nonNull(crossRef.getOrganism()))
                        .collect(
                                Collectors.groupingBy(
                                        UniParcCrossReference::getOrganism, Collectors.counting()));

        return taxonCount.entrySet().stream()
                .sorted(Map.Entry.<Organism, Long>comparingByValue().reversed())
                .map(this::getOrganismFacetItem)
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

    private FacetItem getOrganismFacetItem(Map.Entry<Organism, Long> taxonCount) {
        Organism organism = taxonCount.getKey();
        String organismName =
                organism.hasCommonName() ? organism.getCommonName() : organism.getScientificName();
        if (Utils.nullOrEmpty(organismName)) {
            return null;
        }
        return FacetItem.builder()
                .label(organismName)
                .value(String.valueOf(organism.getTaxonId()))
                .count(taxonCount.getValue())
                .build();
    }

    private FacetItem getStatusFacetItem(Map.Entry<String, Long> statusCount) {
        String value =
                ACTIVE_STR.equals(statusCount.getKey())
                        ? Boolean.TRUE.toString()
                        : Boolean.FALSE.toString();
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
