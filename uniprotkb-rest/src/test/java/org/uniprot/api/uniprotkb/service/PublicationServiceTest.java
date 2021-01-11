package org.uniprot.api.uniprotkb.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.PublicationRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.PublicationSolrQueryConfig;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.publication.UniProtKBMappedReference;
import org.uniprot.core.publication.impl.MappedPublicationsBuilder;
import org.uniprot.core.publication.impl.MappedSourceBuilder;
import org.uniprot.core.publication.impl.UniProtKBMappedReferenceBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.store.search.document.publication.PublicationDocument;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.UniprotKBObjectsForTests.getLiteratureDocument;
import static org.uniprot.store.indexer.publication.common.PublicationUtils.asBinary;

/**
 * @author lgonzales
 * @since 2019-12-17
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PublicationService.class})
@TestPropertySource(
        locations = "/common-message.properties",
        properties = {
            "search.default.page.size=25",
        })
class PublicationServiceTest {
    @MockBean private PublicationRepository publicationRepository;
    @MockBean private LiteratureRepository literatureRepository;
    @MockBean private PublicationConverter publicationConverter;
    @MockBean private LiteratureStoreEntryConverter literatureStoreEntryConverter;
    @MockBean private UniProtKBPublicationsSolrSortClause solrSortClause;
    @MockBean private PublicationFacetConfig publicationFacetConfig;

    @Test
    void ensureHappyPathThroughServiceCallExists() {
        // when
        UniProtKBEntry entry =
                UniprotKBObjectsForTests.getUniprotEntryForPublication("P12345", "200");
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.of(entry));

        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument docForUninprotEntry200 =
                UniprotKBObjectsForTests.getLiteratureDocument(200L);
        when(repository.getAll(argThat(queryContains("id"))))
                .thenAnswer(invocation -> Stream.of(docForUninprotEntry200));

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        service.defaultPageSize = DEFAULT_PAGE_SIZE;
        PublicationRequest request = new PublicationRequest();
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        assertNotNull(result);
        assertNotNull(result.getFacets());
        assertTrue(result.getFacets().isEmpty());

        List<PublicationEntry> entries = result.getContent().collect(Collectors.toList());
        assertNotNull(entries);
        assertEquals(2, entries.size());

        PublicationEntry journal = entries.get(0);
        assertNotNull(journal);
        assertNotNull(journal.getReference());
        assertNotNull(journal.getReference().getCitation());
        assertTrue(journal.getReference().getCitation() instanceof Literature);
        assertNull(journal.getLiteratureMappedReference());
        assertEquals("UniProtKB reviewed (Swiss-Prot)", journal.getPublicationSource());
        assertTrue(journal.getCategories().containsAll(Arrays.asList("Pathol", "Interaction")));

        PublicationEntry submission = entries.get(1);
        assertNotNull(submission);
        assertNull(submission.getLiteratureMappedReference());
        assertNotNull(submission.getReference());
        assertNotNull(submission.getReference().getCitation());
        assertTrue(submission.getReference().getCitation() instanceof Submission);
        assertEquals("UniProtKB reviewed (Swiss-Prot)", submission.getPublicationSource());
        assertEquals(1, submission.getCategories().size());
        assertTrue(submission.getCategories().contains("Interaction"));
    }

    @Test
    void getPublicationsByUniprotAccessionCanReturnMappedPublications() {
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.empty());

        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument docForMappedAccession =
                UniprotKBObjectsForTests.getLiteratureDocument(10L);
        when(repository.getAll(argThat(queryContains("mapped_protein"))))
                .thenAnswer(invocation -> Stream.of(docForMappedAccession));

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        service.defaultPageSize = DEFAULT_PAGE_SIZE;
        PublicationRequest request = new PublicationRequest();
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        assertNotNull(result.getFacets());
        assertTrue(result.getFacets().isEmpty());

        List<PublicationEntry> entries = result.getContent().collect(Collectors.toList());
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
        UniProtKBEntry entry =
                UniprotKBObjectsForTests.getUniprotEntryForPublication("P12345", "200");
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.of(entry));

        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument docForUninprotEntry200 =
                UniprotKBObjectsForTests.getLiteratureDocument(200L);
        when(repository.getAll(argThat(queryContains("id"))))
                .thenAnswer(invocation -> Stream.of(docForUninprotEntry200));

        LiteratureDocument docForMappedAccession =
                UniprotKBObjectsForTests.getLiteratureDocument(10L);
        when(repository.getAll(argThat(queryContains("mapped_protein"))))
                .thenAnswer(invocation -> Stream.of(docForMappedAccession));

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        service.defaultPageSize = DEFAULT_PAGE_SIZE;
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category,source,study_type");
        request.setQuery("category:Interaction");
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);

        assertNotNull(result);
        assertNotNull(result.getContent());
        assertEquals(2, result.getContent().count());
        assertNotNull(result.getFacets());

        List<Facet> facets = new ArrayList<>(result.getFacets());
        assertEquals(3, facets.size());

        assertEquals("source", facets.get(0).getName());
        assertEquals("category", facets.get(1).getName());
        assertEquals("study_type", facets.get(2).getName());
    }

    @Test
    void getPublicationsByUniprotAccessionPaginationFirstPage() {
        UniProtKBStoreClient storeClient = mock(UniProtKBStoreClient.class);
        when(storeClient.getEntry("P12345")).thenReturn(Optional.empty());

        LiteratureRepository repository = getMockedPaginationRepository();

        PublicationService service =
                new PublicationService(
                        storeClient, repository, new LiteratureStoreEntryConverter());
        service.defaultPageSize = DEFAULT_PAGE_SIZE;
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category,source,study_type");
        request.setSize(3);
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniprotAccession("P12345", request);
        assertNotNull(result);
        assertNotNull(result.getFacets());
        assertTrue(result.getFacets().isEmpty());

        List<PublicationEntry> entries = result.getContent().collect(Collectors.toList());
        assertNotNull(entries);
        assertEquals(1, entries.size());

    private LiteratureRepository getMockedPaginationRepository() {
        LiteratureRepository repository = mock(LiteratureRepository.class);
        LiteratureDocument d1 = UniprotKBObjectsForTests.getLiteratureDocument(10L);
        LiteratureDocument d2 = UniprotKBObjectsForTests.getLiteratureDocument(20L);
        LiteratureDocument d3 = UniprotKBObjectsForTests.getLiteratureDocument(30L);
        LiteratureDocument d4 = UniprotKBObjectsForTests.getLiteratureDocument(40L);
        LiteratureDocument d5 = UniprotKBObjectsForTests.getLiteratureDocument(50L);
        LiteratureDocument d6 = UniprotKBObjectsForTests.getLiteratureDocument(60L);
        LiteratureDocument d7 = UniprotKBObjectsForTests.getLiteratureDocument(70L);
        when(repository.getAll(argThat(queryContains("mapped_protein"))))
                .thenAnswer(invocation -> Stream.of(d1, d2, d3, d4, d5, d6, d7));
        return repository;
    }

    private ArgumentMatcher<SolrRequest> queryContains(String queryContains) {
        return solrRequest -> {
            boolean result = false;
            if (solrRequest != null) {
                result = solrRequest.getQuery().contains(queryContains);
            }
            return result;
        };
    }
}
