package org.uniprot.api.uniprotkb.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.uniprotkb.UniprotKBObjectsForTests.getCitationXref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.impl.LiteratureBuilder;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.literature.impl.LiteratureStatisticsBuilder;
import org.uniprot.core.uniprotkb.UniProtKBReference;
import org.uniprot.core.uniprotkb.impl.UniProtKBReferenceBuilder;

/**
 * @author lgonzales
 * @since 2019-12-17
 */
class PublicationFacetConfigTest {
//
//    @Test
//    void getFacetsWithNullInformation() {
//        List<Facet> result = PublicationFacetConfig.getFacets(null, null);
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getFacetsWithEmptyInformation() {
//        List<Facet> result = PublicationFacetConfig.getFacets(Collections.emptyList(), "");
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getFacetsForCategory() {
//        List<PublicationEntry> publications = getCategoryPublicationEntries();
//
//        List<Facet> result = PublicationFacetConfig.getFacets(publications, "category");
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Facet category = result.get(0);
//        assertEquals("category", category.getName());
//        assertEquals("Category", category.getLabel());
//        assertNotNull(category.getValues());
//
//        List<FacetItem> items = category.getValues();
//        assertEquals(3, items.size());
//
//        FacetItem first = items.get(0);
//        assertEquals("Function", first.getLabel());
//        assertEquals("function", first.getValue());
//        assertEquals(new Long(2L), first.getCount());
//
//        FacetItem second = items.get(1);
//        assertEquals("Interation", second.getLabel());
//        assertEquals("interation", second.getValue());
//        assertEquals(new Long(1L), second.getCount());
//    }
//
//    @Test
//    void applyFacetFiltersForCategories() {
//        List<PublicationEntry> publications = getCategoryPublicationEntries();
//        PublicationRequest request = new PublicationRequest();
//        request.setFacets("category");
//        request.setQuery("category:ptm");
//
//        PublicationFacetConfig.applyFacetFilters(publications, request);
//
//        assertNotNull(publications);
//        assertEquals(1, publications.size());
//
//        PublicationEntry entry = publications.get(0);
//        assertTrue(entry.getCategories().contains("PTM"));
//    }
//
//    @Test
//    void getFacetsForSource() {
//        List<PublicationEntry> publications = getSourcePublicationEntries();
//
//        List<Facet> result = PublicationFacetConfig.getFacets(publications, "source");
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Facet category = result.get(0);
//        assertEquals("source", category.getName());
//        assertEquals("Source", category.getLabel());
//        assertNotNull(category.getValues());
//
//        List<FacetItem> items = category.getValues();
//        assertEquals(2, items.size());
//
//        FacetItem first = items.get(0);
//        assertEquals("UniProtKB unreviewed (TrEMBL)", first.getLabel());
//        assertEquals("uniprotkb_unreviewed_trembl", first.getValue());
//        assertEquals(new Long(2L), first.getCount());
//
//        FacetItem second = items.get(1);
//        assertEquals("UniProtKB reviewed (Swiss-Prot)", second.getLabel());
//        assertEquals("uniprotkb_reviewed_swissprot", second.getValue());
//        assertEquals(new Long(1L), second.getCount());
//    }
//
//    @Test
//    void applyFacetFiltersForSource() {
//        List<PublicationEntry> publications = getSourcePublicationEntries();
//        PublicationRequest request = new PublicationRequest();
//        request.setFacets("source");
//        request.setQuery("source:uniprotkb_reviewed_swissprot");
//
//        PublicationFacetConfig.applyFacetFilters(publications, request);
//
//        assertNotNull(publications);
//        assertEquals(1, publications.size());
//
//        PublicationEntry entry = publications.get(0);
//        assertEquals("UniProtKB reviewed (Swiss-Prot)", entry.getPublicationSource());
//    }
//
//    @Test
//    void getFacetsForScale() {
//        List<PublicationEntry> publications = getScalePublicationEntries();
//
//        List<Facet> result = PublicationFacetConfig.getFacets(publications, "study_type");
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        Facet category = result.get(0);
//        assertEquals("study_type", category.getName());
//        assertEquals("Study type", category.getLabel());
//        assertNotNull(category.getValues());
//
//        List<FacetItem> items = category.getValues();
//        assertEquals(2, items.size());
//
//        FacetItem first = items.get(0);
//        assertEquals("Small scale", first.getLabel());
//        assertEquals("small_scale", first.getValue());
//        assertEquals(new Long(2L), first.getCount());
//
//        FacetItem second = items.get(1);
//        assertEquals("Large scale", second.getLabel());
//        assertEquals("large_scale", second.getValue());
//        assertEquals(new Long(1L), second.getCount());
//    }
//
//    @Test
//    void applyFacetFiltersForScale() {
//        List<PublicationEntry> publications = getScalePublicationEntries();
//        PublicationRequest request = new PublicationRequest();
//        request.setFacets("study_type");
//        request.setQuery("study_type:small_scale");
//
//        PublicationFacetConfig.applyFacetFilters(publications, request);
//
//        assertNotNull(publications);
//        assertEquals(2, publications.size());
//
//        PublicationEntry entry = publications.get(0);
//        Literature literature = (Literature) entry.getReference().getCitation();
//        assertFalse(entry.isLargeScale());
//        assertEquals(new Long(50), literature.getPubmedId());
//
//        entry = publications.get(1);
//        literature = (Literature) entry.getReference().getCitation();
//        assertFalse(entry.isLargeScale());
//        assertEquals(new Long(49), literature.getPubmedId());
//    }
//
//    @Test
//    void getFacetNames() {
//        PublicationFacetConfig publicationFacetConfig = new PublicationFacetConfig();
//
//        List<String> facetNames = new ArrayList<>(publicationFacetConfig.getFacetNames());
//
//        assertNotNull(facetNames);
//        assertEquals(3, facetNames.size());
//        assertEquals("source", facetNames.get(0));
//        assertEquals("category", facetNames.get(1));
//        assertEquals("study_type", facetNames.get(2));
//    }
//
//    @Test
//    void getFacetPropertyMap() {
//        PublicationFacetConfig publicationFacetConfig = new PublicationFacetConfig();
//        assertNull(publicationFacetConfig.getFacetPropertyMap());
//    }
//
//    private List<PublicationEntry> getSourcePublicationEntries() {
//        List<PublicationEntry> publications = new ArrayList<>();
//        publications.add(
//                PublicationEntry.builder()
//                        .publicationSource("UniProtKB unreviewed (TrEMBL)")
//                        .build());
//        publications.add(
//                PublicationEntry.builder()
//                        .publicationSource("UniProtKB reviewed (Swiss-Prot)")
//                        .build());
//        publications.add(
//                PublicationEntry.builder()
//                        .publicationSource("UniProtKB unreviewed (TrEMBL)")
//                        .build());
//        return publications;
//    }
//
//    private List<PublicationEntry> getCategoryPublicationEntries() {
//        List<PublicationEntry> publications = new ArrayList<>();
//        publications.add(
//                PublicationEntry.builder().categories(Arrays.asList("Function", "PTM")).build());
//        publications.add(
//                PublicationEntry.builder()
//                        .categories(Arrays.asList("Interation", "Function"))
//                        .build());
//        publications.add(PublicationEntry.builder().build());
//        return publications;
//    }
//
//    private List<PublicationEntry> getScalePublicationEntries() {
//        List<PublicationEntry> publications = new ArrayList<>();
//        publications.add(getScalePublicationEntry(51));
//        publications.add(getScalePublicationEntry(50));
//        publications.add(getScalePublicationEntry(49));
//        return publications;
//    }
//
//    private PublicationEntry getScalePublicationEntry(int count) {
//        CrossReference<CitationDatabase> pubmed =
//                getCitationXref(CitationDatabase.PUBMED, String.valueOf(count));
//        Literature literature = new LiteratureBuilder().citationCrossReferencesAdd(pubmed).build();
//        UniProtKBReference reference = new UniProtKBReferenceBuilder().citation(literature).build();
//        LiteratureStatistics largeStat =
//                new LiteratureStatisticsBuilder().reviewedProteinCount(count).build();
//        return PublicationEntry.builder().statistics(largeStat).reference(reference).build();
//    }
}
