package org.uniprot.api.uniprotkb.common.service.precomputed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationRepository;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.search.document.precomputed.PrecomputedAnnotationDocument;

@ExtendWith(MockitoExtension.class)
class PrecomputedUniProtKBEntryServiceTest {
    @Mock private PrecomputedAnnotationStoreClient storeClient;
    @Mock private PrecomputedAnnotationRepository repository;
    @Mock private ProteomeTaxonomyResolver proteomeTaxonomyResolver;
    @Mock private RequestConverter requestConverter;

    @Test
    void searchReturnsStoreEntriesForPrecomputedDocuments() {
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setUpId("UP000000000");
        request.setCursor("cursor");
        request.setSize(2);
        PrecomputedAnnotationDocument document1 = document("P12345");
        PrecomputedAnnotationDocument document2 = document("Q12345");
        CursorPage page = CursorPage.of(null, 2);
        List<Suggestion> suggestions =
                List.of(Suggestion.builder().query("taxonomy_id:9606").hits(2).build());
        List<ProblemPair> warnings = List.of(new ProblemPair(20, "warning message"));
        QueryResult<PrecomputedAnnotationDocument> documents =
                QueryResult.<PrecomputedAnnotationDocument>builder()
                        .content(Stream.of(document1, document2))
                        .page(page)
                        .suggestions(suggestions)
                        .warnings(warnings)
                        .build();
        UniProtKBEntry entry1 = entry("P12345");
        UniProtKBEntry entry2 = entry("Q12345");
        SolrRequest solrRequest = SolrRequest.builder().query("taxonomy_id:9606").rows(2).build();

        when(proteomeTaxonomyResolver.findTaxonomyIdByUpId("UP000000000")).thenReturn("9606");
        when(requestConverter.createSearchSolrRequest(request)).thenReturn(solrRequest);
        when(repository.searchPage(solrRequest, "cursor")).thenReturn(documents);
        when(storeClient.getEntries(List.of("P12345", "Q12345")))
                .thenReturn(List.of(entry1, entry2));

        QueryResult<UniProtKBEntry> result = service().search(request);

        assertEquals(List.of(entry1, entry2), result.getContent().collect(Collectors.toList()));
        assertSame(page, result.getPage());
        assertSame(suggestions, result.getSuggestions());
        assertSame(warnings, result.getWarnings());

        verify(proteomeTaxonomyResolver).findTaxonomyIdByUpId("UP000000000");
        verify(requestConverter).createSearchSolrRequest(request);
        verify(repository).searchPage(solrRequest, "cursor");
        verify(storeClient).getEntries(List.of("P12345", "Q12345"));
        verify(storeClient, never()).getEntryMap(any());
    }

    @Test
    void searchReturnsEmptyResultWhenSolrDocumentsAreEmpty() {
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setUpId("UP000000000");
        request.setCursor("cursor");
        CursorPage page = CursorPage.of(null, 10);
        SolrRequest solrRequest = SolrRequest.builder().query("taxonomy_id:9606").rows(10).build();
        QueryResult<PrecomputedAnnotationDocument> documents =
                QueryResult.<PrecomputedAnnotationDocument>builder()
                        .content(Stream.empty())
                        .page(page)
                        .build();

        when(proteomeTaxonomyResolver.findTaxonomyIdByUpId("UP000000000")).thenReturn("9606");
        when(requestConverter.createSearchSolrRequest(request)).thenReturn(solrRequest);
        when(repository.searchPage(solrRequest, "cursor")).thenReturn(documents);
        when(storeClient.getEntries(List.of())).thenReturn(List.of());

        QueryResult<UniProtKBEntry> result = service().search(request);

        assertEquals(List.of(), result.getContent().collect(Collectors.toList()));
        assertSame(page, result.getPage());
        verify(storeClient).getEntries(List.of());
    }

    @Test
    void searchReturnsOnlyEntriesAvailableFromStoreClient() {
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setUpId("UP000000000");
        SolrRequest solrRequest = SolrRequest.builder().query("taxonomy_id:9606").rows(25).build();
        QueryResult<PrecomputedAnnotationDocument> documents =
                QueryResult.<PrecomputedAnnotationDocument>builder()
                        .content(Stream.of(document("P12345")))
                        .page(CursorPage.of(null, 2))
                        .build();
        UniProtKBEntry entry = entry("P12345");

        when(proteomeTaxonomyResolver.findTaxonomyIdByUpId("UP000000000")).thenReturn("9606");
        when(requestConverter.createSearchSolrRequest(request)).thenReturn(solrRequest);
        when(repository.searchPage(solrRequest, null)).thenReturn(documents);
        when(storeClient.getEntries(List.of("P12345"))).thenReturn(List.of(entry));

        QueryResult<UniProtKBEntry> result = service().search(request);

        assertEquals(List.of(entry), result.getContent().collect(Collectors.toList()));
        verify(storeClient).getEntries(List.of("P12345"));
    }

    private PrecomputedUniProtKBEntryService service() {
        return new PrecomputedUniProtKBEntryService(
                storeClient, repository, proteomeTaxonomyResolver, requestConverter);
    }

    private static PrecomputedAnnotationDocument document(String accession) {
        return PrecomputedAnnotationDocument.builder().accession(accession).build();
    }

    private static UniProtKBEntry entry(String accession) {
        return new UniProtKBEntryBuilder(accession, accession, UniProtKBEntryType.SWISSPROT)
                .build();
    }
}
