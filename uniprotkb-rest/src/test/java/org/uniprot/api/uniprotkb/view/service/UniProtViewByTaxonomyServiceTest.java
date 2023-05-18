package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.taxonomy.TaxonomyEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.view.service.UniProtKBViewByTaxonomyService.TOP_LEVEL_PARENT_QUERY;

@ExtendWith(MockitoExtension.class)
class UniProtViewByTaxonomyServiceTest {
    private static final String EMPTY_PARENT_ID = "";
    private static final long TAX_ID_A = 1425170;
    private static final long TAX_ID_B = 9606;
    private static final long TAX_ID_C = 21;
    private static final long TAX_ID_D = 100;
    private static final String TAX_ID_A_STRING = String.valueOf(TAX_ID_A);
    private static final String TAX_ID_B_STRING = String.valueOf(TAX_ID_B);
    private static final String TAX_ID_C_STRING = String.valueOf(TAX_ID_C);
    private static final String TAX_ID_D_STRING = String.valueOf(TAX_ID_D);
    public static final String PARENT_TAX_ID_A = ("parent:" + TAX_ID_A);
    public static final String PARENT_TAX_ID_B = ("parent:" + TAX_ID_B);
    public static final String PARENT_TAX_ID_D = ("parent:" + TAX_ID_D);
    private static final String TAX_LABEL_A = "taxLabelA";
    private static final String TAX_LABEL_B = "taxLabelB";
    private static final String TAX_LABEL_C = "taxLabelC";
    private static final String TAX_LABEL_D = "taxLabelD";
    private static final TaxonomyEntry TAXONOMY_ENTRY_A = getTaxonomyEntry(TAX_ID_A, TAX_LABEL_A);
    private static final TaxonomyEntry TAXONOMY_ENTRY_B = getTaxonomyEntry(TAX_ID_B, TAX_LABEL_B);
    private static final TaxonomyEntry TAXONOMY_ENTRY_C = getTaxonomyEntry(TAX_ID_C, TAX_LABEL_C);
    private static final TaxonomyEntry TAXONOMY_ENTRY_D = getTaxonomyEntry(TAX_ID_D, TAX_LABEL_D);
    private static final long TAX_COUNT_A = 23L;
    private static final long TAX_COUNT_B = 50L;
    private static final long TAX_COUNT_C = 9999L;
    private static final long TAX_COUNT_D = 10L;
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_A =
            Map.of(String.valueOf(TAX_ID_A), TAX_COUNT_A);
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_B =
            Map.of(String.valueOf(TAX_ID_B), TAX_COUNT_B);
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_C =
            Map.of(String.valueOf(TAX_ID_C), TAX_COUNT_C);
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_D =
            Map.of(String.valueOf(TAX_ID_D), TAX_COUNT_D);
    private static final Map<String, Long> MULTIPLE_TAXONOMY_FACET_COUNTS =
            Map.of(String.valueOf(TAX_ID_A), TAX_COUNT_A, String.valueOf(TAX_ID_C), TAX_COUNT_C);
    private static final String SOME_QUERY = "someQuery";
    private static final String TAXONOMY_ID = "taxonomy_id";
    private static final String UNIPROT = "uniprot";
    @Mock
    private SolrClient solrClient;
    @Mock
    private TaxonomyService taxonService;
    private UniProtKBViewByTaxonomyService service;

    @BeforeEach
    void setup() {
        service = new UniProtKBViewByTaxonomyService(solrClient, UNIPROT, taxonService);
    }

    @Test
    void get_whenNoParentSpecifiedAndMultipleRootNodes() throws Exception {
        when(taxonService.stream(
                argThat(
                        arg ->
                                Set.of(TOP_LEVEL_PARENT_QUERY, PARENT_TAX_ID_A)
                                        .contains(arg.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient, TAXONOMY_ID, MULTIPLE_TAXONOMY_FACET_COUNTS);

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithNoChildren() throws Exception {
        when(taxonService.stream(argThat(argument -> (TOP_LEVEL_PARENT_QUERY).equals(argument.getQuery())))).thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_C));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_C);

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithMultipleChildren() throws Exception {
        when(taxonService.stream(argThat(argument -> argument != null && (TOP_LEVEL_PARENT_QUERY).equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_B));
        when(taxonService.stream(
                argThat(
                        argument ->
                                argument != null && Set.of(PARENT_TAX_ID_A, PARENT_TAX_ID_B)
                                        .contains(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                MULTIPLE_TAXONOMY_FACET_COUNTS,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_A_STRING)
                                && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilEdge()
            throws Exception {
        when(taxonService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest = invocation.getArgument(0, StreamRequest.class);
                            if (TOP_LEVEL_PARENT_QUERY.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_A);
                            }
                            if (PARENT_TAX_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_B);
                            }
                            if (PARENT_TAX_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_C);
                            }
                            return Stream.of();
                        });
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_A,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_A_STRING));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_C,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys);
    }

    @Test
    void
    get_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilANodeWithMultipleChildren()
            throws Exception {
        when(taxonService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest = invocation.getArgument(0, StreamRequest.class);
                            if (TOP_LEVEL_PARENT_QUERY.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_B);
                            }
                            if (PARENT_TAX_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_D);
                            }
                            if (PARENT_TAX_ID_D.equals(streamRequest.getQuery()) || PARENT_TAX_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C);
                            }
                            return Stream.of();
                        });
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_D,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_D_STRING));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                MULTIPLE_TAXONOMY_FACET_COUNTS,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_A_STRING)
                                && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenParentSpecifiedAndMultipleRootNodes() throws Exception {
        when(taxonService.stream(
                argThat(
                        argument ->
                                Set.of(PARENT_TAX_ID_B, PARENT_TAX_ID_A)
                                        .contains(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient, TAXONOMY_ID, MULTIPLE_TAXONOMY_FACET_COUNTS);

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, TAX_ID_B_STRING);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenParentSpecifiedAndNoRootNodes() {
        when(taxonService.stream(argThat(argument -> PARENT_TAX_ID_A.equals(argument.getQuery())))).thenAnswer(invocation -> Stream.of());

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, TAX_ID_A_STRING);

        assertThat(viewBys, empty());
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_doNotTraverseMore()
            throws Exception {
        when(taxonService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest = invocation.getArgument(0, StreamRequest.class);
                            if (PARENT_TAX_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_B);
                            }
                            if (PARENT_TAX_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_C);
                            }
                            return Stream.of();
                        });
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(
                solrClient,
                TAXONOMY_ID,
                SINGLE_TAXONOMY_FACET_COUNTS_C,
                argument ->
                        argument != null
                                && argument.getFacetFields() != null
                                && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.getViewBys(SOME_QUERY, TAX_ID_A_STRING);

        assertViewByB(viewBys);
    }

    private static TaxonomyEntry getTaxonomyEntry(long id, String label) {
        TaxonomyEntry taxonomyEntry = mock(TaxonomyEntry.class);
        when(taxonomyEntry.getTaxonId()).thenReturn(id);
        when(taxonomyEntry.getScientificName()).thenReturn(label);
        return taxonomyEntry;
    }

    private static void assertViewBysMultiple(List<ViewBy> viewBys) {
        assertThat(viewBys, contains(getViewByA(), getViewByC()));
    }

    private static ViewBy getViewByC() {
        return getViewBy(String.valueOf(TAX_ID_C), TAX_LABEL_C, TAX_COUNT_C, false);
    }

    private static void assertViewByB(List<ViewBy> viewBys) {
        assertThat(viewBys, contains(getViewByB()));
    }

    private static void assertViewByC(List<ViewBy> viewBys) {
        assertThat(viewBys, contains(getViewByC()));
    }

    private static ViewBy getViewByA() {
        return getViewBy(String.valueOf(TAX_ID_A), TAX_LABEL_A, TAX_COUNT_A, true);
    }

    private static ViewBy getViewByB() {
        return getViewBy(String.valueOf(TAX_ID_B), TAX_LABEL_B, TAX_COUNT_B, true);
    }

    private static ViewBy getViewBy(String taxId, String taxLabel, long taxCount, boolean expand) {
        return MockServiceHelper.createViewBy(
                taxId, taxLabel, taxCount, UniProtKBViewByTaxonomyService.URL_PREFIX + taxId, expand);
    }
}
