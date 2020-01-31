package org.uniprot.api.uniprotkb.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniprotkb.UniprotKbObjectsForTests;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.Submission;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-12-17
 */
class PublicationServiceTest {

    @Test
    void getPublicationsByUniprotAccessionCanReturnUniprotEntryPublications() {
        // when
        UniProtEntry entry =
                UniprotKbObjectsForTests.getUniprotEntryForPublication("P12345", "200");
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.of(entry));

        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument docForUninprotEntry200 =
                UniprotKbObjectsForTests.getLiteratureDocument(200L);
        when(repository.getAll(argThat(queryContains("id"))))
                .thenAnswer(invocation -> Stream.of(docForUninprotEntry200));

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        PublicationRequest request = new PublicationRequest();
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        assertNotNull(result);
        assertNotNull(result.getFacets());
        assertTrue(result.getFacets().isEmpty());

        List<PublicationEntry> entries = new ArrayList<>(result.getContent());
        assertNotNull(entries);
        assertEquals(2, entries.size());

        PublicationEntry journal = entries.get(0);
        assertNotNull(journal);
        assertNotNull(journal.getReference());
        assertNotNull(journal.getReference().getCitation());
        assertTrue(journal.getReference().getCitation() instanceof Literature);
        assertNull(journal.getLiteratureMappedReference());
        assertEquals("Swiss-Prot", journal.getPublicationSource());
        assertTrue(journal.getCategories().containsAll(Arrays.asList("Pathol", "Interaction")));

        PublicationEntry submission = entries.get(1);
        assertNotNull(submission);
        assertNull(submission.getLiteratureMappedReference());
        assertNotNull(submission.getReference());
        assertNotNull(submission.getReference().getCitation());
        assertTrue(submission.getReference().getCitation() instanceof Submission);
        assertEquals("Swiss-Prot", submission.getPublicationSource());
        assertEquals(1, submission.getCategories().size());
        assertTrue(submission.getCategories().contains("Interaction"));
    }

    @Test
    void getPublicationsByUniprotAccessionCanReturnMappedPublications() {
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.empty());

        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument docForMappedAccession =
                UniprotKbObjectsForTests.getLiteratureDocument(10L, "P12345");
        when(repository.getAll(argThat(queryContains("mapped_protein"))))
                .thenAnswer(invocation -> Stream.of(docForMappedAccession));

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        PublicationRequest request = new PublicationRequest();
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        assertNotNull(result.getFacets());
        assertTrue(result.getFacets().isEmpty());

        List<PublicationEntry> entries = new ArrayList<>(result.getContent());
        assertNotNull(entries);
        assertEquals(1, entries.size());

        PublicationEntry journal = entries.get(0);
        assertNotNull(journal);
        assertNotNull(journal.getReference());
        assertNotNull(journal.getReference().getCitation());
        assertTrue(journal.getReference().getCitation() instanceof Literature);
        assertNotNull(journal.getLiteratureMappedReference());
        assertEquals("Computationally mapped", journal.getPublicationSource());
        assertTrue(journal.getCategories().contains("Function"));
    }

    @Test
    void getPublicationsByUniprotAccessionWithAllFacets() {
        UniProtEntry entry =
                UniprotKbObjectsForTests.getUniprotEntryForPublication("P12345", "200");
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.of(entry));

        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument docForUninprotEntry200 =
                UniprotKbObjectsForTests.getLiteratureDocument(200L);
        when(repository.getAll(argThat(queryContains("id"))))
                .thenAnswer(invocation -> Stream.of(docForUninprotEntry200));

        LiteratureDocument docForMappedAccession =
                UniprotKbObjectsForTests.getLiteratureDocument(10L, "P12345");
        when(repository.getAll(argThat(queryContains("mapped_protein"))))
                .thenAnswer(invocation -> Stream.of(docForMappedAccession));

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category,source,scale");
        request.setQuery("category:Interaction");
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        assertNotNull(result);
        assertNotNull(result.getContent());
        assertEquals(2, result.getContent().size());
        assertNotNull(result.getFacets());

        List<Facet> facets = new ArrayList<>(result.getFacets());
        assertEquals(3, facets.size());

        assertEquals("source", facets.get(0).getName());
        assertEquals("category", facets.get(1).getName());
        assertEquals("scale", facets.get(2).getName());
    }

    @Test
    void getPublicationsByUniprotAccessionPaginationFirstPage() {
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.empty());

        LiteratureRepository repository = getMockedPaginationRepository();

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category,source,scale");
        request.setSize(3);
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);
        assertNotNull(result);

        CursorPage cursorPage = (CursorPage) result.getPage();
        assertTrue(cursorPage.hasNextPage());
        assertEquals("jxzylcj10", cursorPage.getEncryptedNextCursor());
        assertEquals(new Long(7), cursorPage.getTotalElements());
        assertEquals(new Integer(3), cursorPage.getPageSize());
        assertEquals(new Long(0), cursorPage.getOffset());

        List<PublicationEntry> entries = new ArrayList<>(result.getContent());
        assertNotNull(entries);
        assertEquals(3, entries.size());
        Literature literature = (Literature) entries.get(0).getReference().getCitation();
        assertEquals(new Long(10), literature.getPubmedId());

        literature = (Literature) entries.get(1).getReference().getCitation();
        assertEquals(new Long(20), literature.getPubmedId());

        literature = (Literature) entries.get(2).getReference().getCitation();
        assertEquals(new Long(30), literature.getPubmedId());
    }

    @Test
    void getPublicationsByUniprotAccessionPaginationMiddlePage() {
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.empty());

        LiteratureRepository repository = getMockedPaginationRepository();

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category,source,scale");
        request.setSize(3);
        request.setCursor("jxzylcj10");

        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);
        CursorPage cursorPage = (CursorPage) result.getPage();
        assertTrue(cursorPage.hasNextPage());
        assertEquals("l43abuo2c", cursorPage.getEncryptedNextCursor());
        assertEquals(new Long(7), cursorPage.getTotalElements());
        assertEquals(new Integer(3), cursorPage.getPageSize());
        assertEquals(new Long(3), cursorPage.getOffset());

        List<PublicationEntry> entries = new ArrayList<>(result.getContent());
        assertNotNull(entries);
        assertEquals(3, entries.size());
        Literature literature = (Literature) entries.get(0).getReference().getCitation();
        assertEquals(new Long(40), literature.getPubmedId());

        literature = (Literature) entries.get(1).getReference().getCitation();
        assertEquals(new Long(50), literature.getPubmedId());

        literature = (Literature) entries.get(2).getReference().getCitation();
        assertEquals(new Long(60), literature.getPubmedId());
    }

    @Test
    void getPublicationsByUniprotAccessionPaginationLastPage() {
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.empty());

        LiteratureRepository repository = getMockedPaginationRepository();

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category,source,scale");
        request.setSize(3);
        request.setCursor("l43abuo2c");

        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        CursorPage cursorPage = (CursorPage) result.getPage();
        assertFalse(cursorPage.hasNextPage());
        assertEquals(new Long(7), cursorPage.getTotalElements());
        assertEquals(new Integer(3), cursorPage.getPageSize());
        assertEquals(new Long(6), cursorPage.getOffset());

        List<PublicationEntry> entries = new ArrayList<>(result.getContent());
        assertNotNull(entries);
        assertEquals(1, entries.size());
        Literature literature = (Literature) entries.get(0).getReference().getCitation();
        assertEquals(new Long(70), literature.getPubmedId());
    }

    private LiteratureRepository getMockedPaginationRepository() {
        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument d1 = UniprotKbObjectsForTests.getLiteratureDocument(10L, "P12345");
        LiteratureDocument d2 = UniprotKbObjectsForTests.getLiteratureDocument(20L, "P12345");
        LiteratureDocument d3 = UniprotKbObjectsForTests.getLiteratureDocument(30L, "P12345");
        LiteratureDocument d4 = UniprotKbObjectsForTests.getLiteratureDocument(40L, "P12345");
        LiteratureDocument d5 = UniprotKbObjectsForTests.getLiteratureDocument(50L, "P12345");
        LiteratureDocument d6 = UniprotKbObjectsForTests.getLiteratureDocument(60L, "P12345");
        LiteratureDocument d7 = UniprotKbObjectsForTests.getLiteratureDocument(70L, "P12345");
        when(repository.getAll(argThat(queryContains("mapped_protein"))))
                .thenAnswer(invocation -> Stream.of(d1, d2, d3, d4, d5, d6, d7));
        return repository;
    }

    private ArgumentMatcher<SolrRequest> queryContains(String queryContains) {
        return new ArgumentMatcher<SolrRequest>() {
            @Override
            public boolean matches(SolrRequest solrRequest) {
                boolean result = false;
                if (solrRequest != null) {
                    result = solrRequest.getQuery().contains(queryContains);
                }
                return result;
            }
        };
    }
}
