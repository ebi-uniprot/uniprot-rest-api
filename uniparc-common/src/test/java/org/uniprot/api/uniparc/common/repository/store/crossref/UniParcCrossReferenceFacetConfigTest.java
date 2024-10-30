package org.uniprot.api.uniparc.common.repository.store.crossref;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceFacetConfig.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;

class UniParcCrossReferenceFacetConfigTest {

    @Test
    void testEmptyFacets() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        String commaSeparatedFacetNames = String.join(",", facetConfig.getFacetNames());
        List<Facet> facets =
                facetConfig.getUniParcCrossReferenceFacets(Stream.of(), commaSeparatedFacetNames);
        assertNotNull(facets);
        assertTrue(facets.isEmpty());
    }

    @Test
    void testAllFacetsSuccess() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        String commaSeparatedFacetNames = String.join(",", facetConfig.getFacetNames());
        List<UniParcCrossReference> crossReferences =
                UniParcCrossReferenceMocker.createCrossReferences(43, 25);
        List<Facet> facets =
                facetConfig.getUniParcCrossReferenceFacets(
                        crossReferences.stream(), commaSeparatedFacetNames);
        assertNotNull(facets);
        assertEquals(3, facets.size());
        // verify status facet
        Facet statusFacet = facets.get(0);
        assertEquals(
                UniParcCrossReferenceFacetConfig.UniParcCrossReferenceFacet.STATUS.getLabel(),
                statusFacet.getLabel());
        assertEquals(ACTIVE_FIELD, statusFacet.getName());
        assertEquals(2, statusFacet.getValues().size());
        FacetItem activeFacetItem = statusFacet.getValues().get(0);
        assertEquals(ACTIVE_STR, activeFacetItem.getLabel());
        assertEquals(Boolean.TRUE.toString(), activeFacetItem.getValue());
        assertEquals(21L, activeFacetItem.getCount());
        FacetItem inactiveFacetItem = statusFacet.getValues().get(1);
        assertEquals(INACTIVE_STR, inactiveFacetItem.getLabel());
        assertEquals(Boolean.FALSE.toString(), inactiveFacetItem.getValue());
        assertEquals(4L, inactiveFacetItem.getCount());

        // verify organism facet
        Facet organismFacet = facets.get(1);
        assertEquals(
                UniParcCrossReferenceFacetConfig.UniParcCrossReferenceFacet.ORGANISMS.getLabel(),
                organismFacet.getLabel());
        assertEquals(TAXONIDS_FIELD, organismFacet.getName());
        assertEquals(2, organismFacet.getValues().size());
        FacetItem organismFacetItem = organismFacet.getValues().get(0);
        assertEquals("Name 7787", organismFacetItem.getLabel());
        assertEquals("7787", organismFacetItem.getValue());
        assertEquals(5L, organismFacetItem.getCount());

        // verify databases facet and swiss prot and trembl should be first two
        Facet dbFacet = facets.get(2);
        assertEquals(
                UniParcCrossReferenceFacetConfig.UniParcCrossReferenceFacet.DATABASES.getLabel(),
                dbFacet.getLabel());
        assertEquals(DBTYPES_FIELD, dbFacet.getName());
        assertEquals(6, dbFacet.getValues().size());
        FacetItem swissProtFacetItem = dbFacet.getValues().get(0);
        assertEquals(UniParcDatabase.SWISSPROT.getCompareOn(), swissProtFacetItem.getLabel());
        assertEquals(UniParcDatabase.SWISSPROT.getCompareOn(), swissProtFacetItem.getValue());
        assertEquals(5L, swissProtFacetItem.getCount());
        FacetItem uniProtFacetItem = dbFacet.getValues().get(1);
        assertEquals(UniParcDatabase.TREMBL.getCompareOn(), uniProtFacetItem.getLabel());
        assertEquals(UniParcDatabase.TREMBL.getCompareOn(), uniProtFacetItem.getValue());
        assertEquals(4L, uniProtFacetItem.getCount());
    }

    @Test
    void testFacetItemStatuses() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        List<UniParcCrossReference> crossReferences =
                UniParcCrossReferenceMocker.createCrossReferences(10, 5);
        List<FacetItem> statusFacetItems = facetConfig.getFacetItemStatuses(crossReferences);

        assertEquals(2, statusFacetItems.size());
        assertEquals(ACTIVE_STR, statusFacetItems.get(0).getLabel());
        assertEquals(Boolean.TRUE.toString(), statusFacetItems.get(0).getValue());
        assertEquals(4L, statusFacetItems.get(0).getCount());
        assertEquals(INACTIVE_STR, statusFacetItems.get(1).getLabel());
        assertEquals(1L, statusFacetItems.get(1).getCount());
        assertEquals(Boolean.FALSE.toString(), statusFacetItems.get(1).getValue());
    }

    @Test
    void testFacetItemOrganisms() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        List<UniParcCrossReference> crossReferences =
                UniParcCrossReferenceMocker.createCrossReferences(20, 10);
        List<FacetItem> organismFacetItems = facetConfig.getFacetItemOrganisms(crossReferences);

        assertEquals(2, organismFacetItems.size());
        assertEquals("Name 9606", organismFacetItems.get(0).getLabel());
        assertEquals("9606", organismFacetItems.get(0).getValue());
        assertEquals(2L, organismFacetItems.get(0).getCount());
        assertEquals("Name 7787", organismFacetItems.get(1).getLabel());
        assertEquals("7787", organismFacetItems.get(1).getValue());
        assertEquals(2L, organismFacetItems.get(1).getCount());
    }

    @Test
    void testFacetItemOrganismsWithAndWithoutCommonNameButScientificName() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        List<UniParcCrossReference> crossReferences =
                UniParcCrossReferenceMocker.createCrossReferences(20, 10);
        UniParcCrossReference xrefWithCommonName =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.FLYBASE, "123", 123, true);
        Organism organism =
                OrganismBuilder.from(xrefWithCommonName.getOrganism())
                        .commonName("Common " + xrefWithCommonName.getOrganism().getTaxonId())
                        .build();
        xrefWithCommonName =
                UniParcCrossReferenceBuilder.from(xrefWithCommonName).organism(organism).build();
        crossReferences.add(xrefWithCommonName);
        List<FacetItem> organismFacetItems = facetConfig.getFacetItemOrganisms(crossReferences);

        assertEquals(3, organismFacetItems.size());
        assertEquals("Name 9606", organismFacetItems.get(0).getLabel());
        assertEquals("9606", organismFacetItems.get(0).getValue());
        assertEquals(2L, organismFacetItems.get(0).getCount());
        assertEquals("Name 7787", organismFacetItems.get(1).getLabel());
        assertEquals("7787", organismFacetItems.get(1).getValue());
        assertEquals(2L, organismFacetItems.get(1).getCount());
        assertEquals("Common 123", organismFacetItems.get(2).getLabel());
        assertEquals("123", organismFacetItems.get(2).getValue());
        assertEquals(1L, organismFacetItems.get(2).getCount());
    }

    @Test
    void testFacetWithoutCommonNameOrScientificName() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        String commaSeparatedFacetNames = String.join(",", facetConfig.getFacetNames());
        UniParcCrossReference xrefWithoutName =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.FLYBASE, "123", 123, true);
        Organism organism =
                OrganismBuilder.from(xrefWithoutName.getOrganism())
                        .commonName(null)
                        .scientificName(null)
                        .build();
        xrefWithoutName =
                UniParcCrossReferenceBuilder.from(xrefWithoutName).organism(organism).build();
        List<Facet> facets =
                facetConfig.getUniParcCrossReferenceFacets(
                        Stream.of(xrefWithoutName), commaSeparatedFacetNames);
        assertEquals(2, facets.size());
        assertEquals(
                UniParcCrossReferenceFacetConfig.UniParcCrossReferenceFacet.STATUS.getLabel(),
                facets.get(0).getLabel());
        assertEquals(UniParcCrossReferenceFacet.DATABASES.getLabel(), facets.get(1).getLabel());
    }

    @Test
    void testFacetWithoutDatabase() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        String commaSeparatedFacetNames = String.join(",", facetConfig.getFacetNames());
        UniParcCrossReference xrefWithoutDatabase =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.FLYBASE, "123", 123, true);
        xrefWithoutDatabase =
                UniParcCrossReferenceBuilder.from(xrefWithoutDatabase).database(null).build();
        List<Facet> facets =
                facetConfig.getUniParcCrossReferenceFacets(
                        Stream.of(xrefWithoutDatabase), commaSeparatedFacetNames);
        assertEquals(2, facets.size());
        assertEquals(
                UniParcCrossReferenceFacetConfig.UniParcCrossReferenceFacet.STATUS.getLabel(),
                facets.get(0).getLabel());
        assertEquals(UniParcCrossReferenceFacet.ORGANISMS.getLabel(), facets.get(1).getLabel());
    }

    @Test
    void testFacetItemDatabases() {
        UniParcCrossReferenceFacetConfig facetConfig = new UniParcCrossReferenceFacetConfig();
        List<UniParcCrossReference> crossReferences =
                UniParcCrossReferenceMocker.createCrossReferences(30, 15);
        List<FacetItem> databaseFacetItems = facetConfig.getFacetItemDatabases(crossReferences);

        assertEquals(6, databaseFacetItems.size());
        FacetItem swissProtFacetItem = databaseFacetItems.get(0);
        assertEquals(UniParcDatabase.SWISSPROT.getDisplayName(), swissProtFacetItem.getLabel());
        assertEquals(UniParcDatabase.SWISSPROT.getDisplayName(), swissProtFacetItem.getValue());
        assertEquals(3L, swissProtFacetItem.getCount());

        FacetItem tremblFacetItem = databaseFacetItems.get(1);
        assertEquals(UniParcDatabase.TREMBL.getDisplayName(), tremblFacetItem.getLabel());
        assertEquals(UniParcDatabase.TREMBL.getDisplayName(), tremblFacetItem.getValue());
        assertEquals(3L, tremblFacetItem.getCount());

        FacetItem otherFacetItem = databaseFacetItems.get(2);
        assertEquals("RefSeq", otherFacetItem.getLabel());
        assertEquals(3L, otherFacetItem.getCount());
    }
}
