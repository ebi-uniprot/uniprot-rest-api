package org.uniprot.api.uniprotkb.common.service.publication;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.common.UniProtKBObjectsForTests.getLiteratureDocument;
import static org.uniprot.store.indexer.publication.common.PublicationUtils.asBinary;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.rest.service.query.config.LiteratureSolrQueryConfig;
import org.uniprot.api.rest.service.query.sort.LiteratureSortClause;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniprotkb.common.repository.model.PublicationEntry;
import org.uniprot.api.uniprotkb.common.repository.search.LiteratureRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationSolrQueryConfig;
import org.uniprot.api.uniprotkb.common.service.publication.request.PublicationRequest;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.UniProtKBMappedReference;
import org.uniprot.core.publication.impl.MappedPublicationsBuilder;
import org.uniprot.core.publication.impl.MappedSourceBuilder;
import org.uniprot.core.publication.impl.UniProtKBMappedReferenceBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.store.search.document.publication.PublicationDocument;

/**
 * @author lgonzales
 * @since 2019-12-17
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PublicationService.class})
@TestPropertySource(
        locations = "/common-message.properties",
        properties = {
            "search.request.converter.defaultRestPageSize=25",
        })
class PublicationServiceTest {
    @MockBean private PublicationRepository publicationRepository;
    @MockBean private LiteratureRepository literatureRepository;
    @MockBean private PublicationConverter publicationConverter;
    @MockBean private LiteratureEntryConverter literatureEntryConverter;
    @MockBean private UniProtKBPublicationsSolrSortClause solrSortClause;
    @MockBean private PublicationFacetConfig publicationFacetConfig;

    @Qualifier("publicationRequestConverter")
    @MockBean
    private RequestConverter publicationRequestConverter;

    @MockBean private LiteratureSortClause literatureSortClause;

    @Test
    void ensureHappyPathThroughServiceCallExists() {
        // when
        when(literatureRepository.getAll(argThat(queryContains("id"))))
                .thenAnswer(invocation -> Stream.of(getLiteratureDocument(2L)));

        UniProtKBMappedReference ref =
                new UniProtKBMappedReferenceBuilder()
                        .sourceCategoriesAdd("Interaction")
                        .source(
                                new MappedSourceBuilder()
                                        .name(UniProtKBEntryType.SWISSPROT.getName())
                                        .build())
                        .build();

        MappedPublications mappedPublications =
                new MappedPublicationsBuilder().uniProtKBMappedReference(ref).build();

        PublicationDocument publicationDocument =
                PublicationDocument.builder()
                        .publicationMappedReferences(asBinary(mappedPublications))
                        .citationId("2")
                        .build();

        QueryResult<PublicationDocument> pubDocs =
                QueryResult.<PublicationDocument>builder()
                        .content(Stream.of(publicationDocument))
                        .page(CursorPage.of(null, 1))
                        .facets(emptyList())
                        .build();

        when(publicationRepository.searchPage(any(), eq(null))).thenReturn(pubDocs);

        solrSortClause.init();

        PublicationSolrQueryConfig publicationSolrQueryConfig = new PublicationSolrQueryConfig();
        LiteratureSolrQueryConfig literatureSolrQueryConfig = new LiteratureSolrQueryConfig();

        PublicationService service =
                new PublicationService(
                        publicationRepository,
                        literatureRepository,
                        new PublicationConverter(),
                        new LiteratureEntryConverter(),
                        publicationSolrQueryConfig.publicationQueryProcessorConfig(
                                publicationSolrQueryConfig.publicationSearchFieldConfig()),
                        literatureSolrQueryConfig.literatureSolrQueryConf(),
                        publicationRequestConverter);
        PublicationRequest request = new PublicationRequest();
        request.setSize(25);
        QueryResult<PublicationEntry> result =
                service.getPublicationsByUniProtAccession("P12345", request);

        assertNotNull(result);
        assertNotNull(result.getFacets());
        assertTrue(result.getFacets().isEmpty());

        List<PublicationEntry> entries = result.getContent().collect(Collectors.toList());
        assertNotNull(entries);
        assertEquals(1, entries.size());

        PublicationEntry journal = entries.get(0);
        assertNotNull(journal);
        assertNotNull(journal.getCitation());
        assertTrue(journal.getCitation() instanceof Literature);
        assertNotNull(journal.getReferences());
        assertEquals(
                UniProtKBEntryType.SWISSPROT.getName(),
                journal.getReferences().get(0).getSource().getName());
        assertThat(journal.getReferences().get(0).getSourceCategories(), contains("Interaction"));
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
