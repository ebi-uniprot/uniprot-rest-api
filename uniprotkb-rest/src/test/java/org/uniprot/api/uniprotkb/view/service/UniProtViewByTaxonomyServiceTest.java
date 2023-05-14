package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.view.TaxonomyNode;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniProtViewByTaxonomyServiceTest {
    private static final String EMPTY_PARENT_ID = "";
    private static final String DEFAULT_PARENT_ID = "1";
    private static final long TAX_ID_A = 1425170;
    private static final long TAX_ID_B = 9606;
    private static final long TAX_ID_C = 21;
    private static final long TAX_ID_D = 100;
    private static final String TAX_ID_A_STRING = String.valueOf(TAX_ID_A);
    private static final String TAX_ID_B_STRING = String.valueOf(TAX_ID_B);
    private static final String TAX_ID_C_STRING = String.valueOf(TAX_ID_C);
    private static final String TAX_ID_D_STRING = String.valueOf(TAX_ID_D);
    private static final String TAX_LABEL_A = "taxLabelA";
    private static final String TAX_LABEL_B = "taxLabelB";
    private static final String TAX_LABEL_C = "taxLabelC";
    private static final String TAX_LABEL_D = "taxLabelD";
    private static final TaxonomyNode TAXONOMY_NODE_A = getTaxonomyNode(TAX_ID_A, TAX_LABEL_A);
    private static final TaxonomyNode TAXONOMY_NODE_B = getTaxonomyNode(TAX_ID_B, TAX_LABEL_B);
    private static final TaxonomyNode TAXONOMY_NODE_C = getTaxonomyNode(TAX_ID_C, TAX_LABEL_C);
    private static final TaxonomyNode TAXONOMY_NODE_D = getTaxonomyNode(TAX_ID_D, TAX_LABEL_D);
    private static final List<TaxonomyNode> SINGLE_TAXONOMY_NODE_A = List.of(TAXONOMY_NODE_A);
    private static final List<TaxonomyNode> SINGLE_TAXONOMY_NODE_B = List.of(TAXONOMY_NODE_B);
    private static final List<TaxonomyNode> SINGLE_TAXONOMY_NODE_C = List.of(TAXONOMY_NODE_C);
    private static final List<TaxonomyNode> SINGLE_TAXONOMY_NODE_D = List.of(TAXONOMY_NODE_D);
    private static final List<TaxonomyNode> MULTIPLE_TAXONOMY_NODES = List.of(TAXONOMY_NODE_A, TAXONOMY_NODE_C);
    private static final long TAX_COUNT_A = 23L;
    private static final long TAX_COUNT_B = 50L;
    private static final long TAX_COUNT_C = 9999L;
    private static final long TAX_COUNT_D = 10L;
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_A = Map.of(String.valueOf(TAX_ID_A), TAX_COUNT_A);
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_B = Map.of(String.valueOf(TAX_ID_B), TAX_COUNT_B);
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_C = Map.of(String.valueOf(TAX_ID_C), TAX_COUNT_C);
    private static final Map<String, Long> SINGLE_TAXONOMY_FACET_COUNTS_D = Map.of(String.valueOf(TAX_ID_D), TAX_COUNT_D);
    private static final Map<String, Long> MULTIPLE_TAXONOMY_FACET_COUNTS = Map.of(String.valueOf(TAX_ID_A), TAX_COUNT_A,
            String.valueOf(TAX_ID_C), TAX_COUNT_C);
    private static final String SOME_QUERY = "someQuery";
    private static final String TAXONOMY_ID = "taxonomy_id";
    private static final String UNIPROT = "uniprot";
    @Mock
    private SolrClient solrClient;
    @Mock
    private TaxonomyService taxonService;
    private UniProtViewByTaxonomyService service;

    @BeforeEach
    void setup() {
        service = new UniProtViewByTaxonomyService(solrClient, UNIPROT, taxonService);
    }

    @Test
    void get_whenNoParentSpecifiedAndMultipleRootNodes() throws Exception {
        when(taxonService.getChildren(argThat(arg -> Set.of(DEFAULT_PARENT_ID, String.valueOf(TAX_ID_A)).contains(arg)))).thenReturn(MULTIPLE_TAXONOMY_NODES);
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, MULTIPLE_TAXONOMY_FACET_COUNTS);

        List<ViewBy> viewBys = service.get(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithNoChildren() throws Exception {
        when(taxonService.getChildren(DEFAULT_PARENT_ID)).thenReturn(SINGLE_TAXONOMY_NODE_C);
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_C);

        List<ViewBy> viewBys = service.get(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithMultipleChildren() throws Exception {
        when(taxonService.getChildren(DEFAULT_PARENT_ID)).thenReturn(SINGLE_TAXONOMY_NODE_B);
        when(taxonService.getChildren(argThat(arg -> Set.of(String.valueOf(TAX_ID_B), String.valueOf(TAX_ID_A)).contains(arg)))).thenReturn(MULTIPLE_TAXONOMY_NODES);
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, MULTIPLE_TAXONOMY_FACET_COUNTS,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_A_STRING) && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.get(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilEdge() throws Exception {
        when(taxonService.getChildren(anyString())).thenAnswer(invocation -> {
            String taxId = invocation.getArgument(0, String.class);
            if (DEFAULT_PARENT_ID.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_A;
            }
            if (TAX_ID_A_STRING.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_B;
            }
            if (TAX_ID_B_STRING.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_C;
            }
            return Collections.emptyList();
        });
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_A,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_A_STRING));
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_C,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.get(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys);
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilANodeWithMultipleChildren() throws Exception {
        when(taxonService.getChildren(anyString())).thenAnswer(invocation -> {
            String taxId = invocation.getArgument(0, String.class);
            if (DEFAULT_PARENT_ID.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_B;
            }
            if (TAX_ID_B_STRING.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_D;
            }
            if (TAX_ID_D_STRING.equals(taxId) || TAX_ID_A_STRING.equals(taxId)) {
                return MULTIPLE_TAXONOMY_NODES;
            }
            return Collections.emptyList();
        });
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_D,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_D_STRING));
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, MULTIPLE_TAXONOMY_FACET_COUNTS,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_A_STRING) && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.get(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenParentSpecifiedAndMultipleRootNodes() throws Exception {
        when(taxonService.getChildren(argThat(arg -> Set.of(TAX_ID_B_STRING, String.valueOf(TAX_ID_A)).contains(arg)))).thenReturn(MULTIPLE_TAXONOMY_NODES);
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, MULTIPLE_TAXONOMY_FACET_COUNTS);

        List<ViewBy> viewBys = service.get(SOME_QUERY, TAX_ID_B_STRING);

        assertViewBysMultiple(viewBys);
    }

    @Test
    void get_whenParentSpecifiedAndNoRootNodes() {
        when(taxonService.getChildren(TAX_ID_A_STRING)).thenReturn(Collections.emptyList());

        List<ViewBy> viewBys = service.get(SOME_QUERY, TAX_ID_A_STRING);

        assertThat(viewBys, empty());
    }

    @Test
    void get_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_doNotTraverseMore() throws Exception {
        when(taxonService.getChildren(anyString())).thenAnswer(invocation -> {
            String taxId = invocation.getArgument(0, String.class);
            if (TAX_ID_A_STRING.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_B;
            }
            if (TAX_ID_B_STRING.equals(taxId)) {
                return SINGLE_TAXONOMY_NODE_C;
            }
            return Collections.emptyList();
        });
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_B,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_B_STRING));
        MockServiceHelper.mockServiceQueryResponse(solrClient, TAXONOMY_ID, SINGLE_TAXONOMY_FACET_COUNTS_C,
                argument -> argument != null && argument.getFacetFields() != null && argument.getFacetFields()[0].contains(TAX_ID_C_STRING));

        List<ViewBy> viewBys = service.get(SOME_QUERY, TAX_ID_A_STRING);

        assertViewByB(viewBys);
    }

    private static TaxonomyNode getTaxonomyNode(long id, String label) {
        TaxonomyNode node1 = new TaxonomyNode();
        node1.setTaxonomyId(id);
        node1.setScientificName(label);
        return node1;
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
                taxId,
                taxLabel,
                taxCount,
                UniProtViewByTaxonomyService.URL_PREFIX + taxId,
                expand);
    }
}
