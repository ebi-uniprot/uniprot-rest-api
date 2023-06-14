package org.uniprot.api.uniprotkb.groupby.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.groupby.service.UniProtKBGroupByTaxonomyService.TOP_LEVEL_PARENT_QUERY;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.api.uniprotkb.groupby.model.Ancestor;
import org.uniprot.api.uniprotkb.groupby.model.AncestorImpl;
import org.uniprot.api.uniprotkb.groupby.model.Group;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

@ExtendWith(MockitoExtension.class)
class UniProtKBGroupTaxonomyServiceTest {
    private static final String EMPTY_PARENT_ID = "";
    private static final long TAX_ID_A = 1425170;
    private static final long TAX_ID_B = 9606;
    private static final long TAX_ID_C = 21;
    private static final long TAX_ID_D = 100;
    private static final long TAX_ID_E = 33204;
    private static final String TAX_ID_A_STRING = String.valueOf(TAX_ID_A);
    private static final String TAX_ID_B_STRING = String.valueOf(TAX_ID_B);
    private static final String TAX_ID_C_STRING = String.valueOf(TAX_ID_C);
    private static final String TAX_ID_D_STRING = String.valueOf(TAX_ID_D);
    private static final String TAX_ID_E_STRING = String.valueOf(TAX_ID_E);
    public static final String PARENT_TAX_ID_A = ("parent:" + TAX_ID_A);
    public static final String PARENT_TAX_ID_B = ("parent:" + TAX_ID_B);
    public static final String PARENT_TAX_ID_D = ("parent:" + TAX_ID_D);
    private static final String TAX_LABEL_A = "taxLabelA";
    private static final String TAX_LABEL_B = "taxLabelB";
    private static final String TAX_LABEL_C = "taxLabelC";
    private static final String TAX_LABEL_D = "taxLabelD";
    private static final String TAX_LABEL_E = "taxLabelE";
    private static final TaxonomyEntry TAXONOMY_ENTRY_A = getTaxonomyEntry(TAX_ID_A, TAX_LABEL_A);
    private static final TaxonomyEntry TAXONOMY_ENTRY_B = getTaxonomyEntry(TAX_ID_B, TAX_LABEL_B);
    private static final TaxonomyEntry TAXONOMY_ENTRY_C = getTaxonomyEntry(TAX_ID_C, TAX_LABEL_C);
    private static final TaxonomyEntry TAXONOMY_ENTRY_D = getTaxonomyEntry(TAX_ID_D, TAX_LABEL_D);
    private static final TaxonomyEntry TAXONOMY_ENTRY_E = getTaxonomyEntry(TAX_ID_E, TAX_LABEL_E);
    private static final long TAX_COUNT_A = 23L;
    private static final long TAX_COUNT_B = 50L;
    private static final long TAX_COUNT_C = 9999L;
    private static final long TAX_COUNT_D = 10L;
    private static final long TAX_COUNT_E = 1995L;
    private static final String SOME_NAME = "someName";
    private static final List<FacetField> SINGLE_TAXONOMY_FACET_COUNTS_A =
            getFacetFields(TAX_ID_A_STRING, TAX_COUNT_A);
    private static final List<FacetField> SINGLE_TAXONOMY_FACET_COUNTS_B =
            getFacetFields(TAX_ID_B_STRING, TAX_COUNT_B);
    private static final List<FacetField> SINGLE_TAXONOMY_FACET_COUNTS_C =
            getFacetFields(TAX_ID_C_STRING, TAX_COUNT_C);
    private static final List<FacetField> SINGLE_TAXONOMY_FACET_COUNTS_D =
            getFacetFields(TAX_ID_D_STRING, TAX_COUNT_D);
    private static final List<FacetField> SINGLE_TAXONOMY_FACET_COUNTS_E =
            getFacetFields(TAX_ID_E_STRING, TAX_COUNT_E);
    private static final List<FacetField> MULTIPLE_TAXONOMY_FACET_COUNTS = getMultipleFacetFields();

    private static List<FacetField> getFacetFields(String id, long count) {
        return List.of(
                new FacetField(SOME_NAME) {
                    {
                        add(id, count);
                    }
                });
    }

    private static List<FacetField> getMultipleFacetFields() {
        return List.of(
                new FacetField(SOME_NAME) {
                    {
                        add(String.valueOf(TAX_ID_A), TAX_COUNT_A);
                        add(String.valueOf(TAX_ID_C), TAX_COUNT_C);
                    }
                });
    }

    private static final String SOME_QUERY = "someQuery";
    @Mock private TaxonomyService taxonomyService;
    @Mock private UniProtEntryService uniProtEntryService;
    private UniProtKBGroupByTaxonomyService service;

    @BeforeEach
    void setup() {
        service = new UniProtKBGroupByTaxonomyService(taxonomyService, uniProtEntryService);
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndMultipleRootNodes() {
        when(taxonomyService.stream(
                        argThat(
                                arg ->
                                        arg != null
                                                && TOP_LEVEL_PARENT_QUERY.equals(arg.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C));
        when(taxonomyService.stream(
                        argThat(arg -> arg != null && PARENT_TAX_ID_A.equals(arg.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_B));
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(TAX_ID_A_STRING + "," + TAX_ID_C_STRING)))
                .thenReturn(MULTIPLE_TAXONOMY_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_B_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_B);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithNoChildren() {
        when(taxonomyService.stream(
                        argThat(argument -> (TOP_LEVEL_PARENT_QUERY).equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_C));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_C_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys, empty());
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithMultipleChildren() {
        when(taxonomyService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && (TOP_LEVEL_PARENT_QUERY)
                                                        .equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_B));
        when(taxonomyService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && PARENT_TAX_ID_B.equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C));
        when(taxonomyService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && PARENT_TAX_ID_A.equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_E));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_B_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(TAX_ID_A_STRING + "," + TAX_ID_C_STRING)))
                .thenReturn(MULTIPLE_TAXONOMY_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_E_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB()));
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilEdge() {
        when(taxonomyService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
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
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_A_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_A);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_B_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_C_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys, contains(getAncestorA(), getAncestorB()));
    }

    @Test
    void
            getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilANodeWithMultipleChildren() {
        when(taxonomyService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (TOP_LEVEL_PARENT_QUERY.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_B);
                            }
                            if (PARENT_TAX_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_D);
                            }
                            if (PARENT_TAX_ID_D.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C);
                            }
                            if (PARENT_TAX_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_E);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_B_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_D_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(TAX_ID_A_STRING + "," + TAX_ID_C_STRING)))
                .thenReturn(MULTIPLE_TAXONOMY_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_E_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB(), getAncestorD()));
    }

    @Test
    void getViewBys_whenParentSpecifiedAndMultipleRootNodes() {
        when(taxonomyService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && PARENT_TAX_ID_B.equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C));
        when(taxonomyService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && PARENT_TAX_ID_A.equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(TAXONOMY_ENTRY_E));
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(TAX_ID_A_STRING + "," + TAX_ID_C_STRING)))
                .thenReturn(MULTIPLE_TAXONOMY_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_E_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, TAX_ID_B_STRING);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getViewBys_whenParentSpecifiedAndNoChildNodes() {
        when(taxonomyService.stream(
                        argThat(argument -> PARENT_TAX_ID_A.equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of());

        GroupByResult viewBys = service.getGroups(SOME_QUERY, TAX_ID_A_STRING);

        assertThat(viewBys.getResults(), empty());
        assertThat(viewBys.getAncestors(), empty());
    }

    @Test
    void getViewBys_whenParentSpecifiedAndSingleChildWithSingleChild_traverseUntilEnd() {
        when(taxonomyService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (PARENT_TAX_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_B);
                            }
                            if (PARENT_TAX_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_C);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_B_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_C_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, TAX_ID_A_STRING);

        assertViewByC(viewBys, contains(getAncestorB()));
    }

    @Test
    void
            getViewBys_whenParentSpecifiedAndSingleChildWithMultipleChildren_traverseUntilANodeWithMultipleChildren() {
        when(taxonomyService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (PARENT_TAX_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_D);
                            }
                            if (PARENT_TAX_ID_D.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_A, TAXONOMY_ENTRY_C);
                            }
                            if (PARENT_TAX_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(TAXONOMY_ENTRY_E);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_D_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(TAX_ID_A_STRING + "," + TAX_ID_C_STRING)))
                .thenReturn(MULTIPLE_TAXONOMY_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(TAX_ID_E_STRING)))
                .thenReturn(SINGLE_TAXONOMY_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroups(SOME_QUERY, TAX_ID_B_STRING);

        assertViewBysMultiple(viewBys, contains(getAncestorD()));
    }

    private static Map<String, String> getFacetFields(String facetItems) {
        return Map.of(
                FacetParams.FACET_FIELD, String.format("{!terms='%s'}taxonomy_id", facetItems));
    }

    private static TaxonomyEntry getTaxonomyEntry(long id, String label) {
        TaxonomyEntry taxonomyEntry = mock(TaxonomyEntry.class);
        when(taxonomyEntry.getTaxonId()).thenReturn(id);
        when(taxonomyEntry.getScientificName()).thenReturn(label);
        return taxonomyEntry;
    }

    private static void assertViewBysMultiple(
            GroupByResult groupByResult, Matcher<? super List<Ancestor>> matcher) {
        assertThat(groupByResult.getResults(), contains(getViewByA(), getViewByC()));
        assertThat(groupByResult.getAncestors(), matcher);
    }

    private static void assertViewByC(
            GroupByResult viewBys, Matcher<? super List<Ancestor>> matcher) {
        assertThat(viewBys.getResults(), contains(getViewByC()));
        assertThat(viewBys.getAncestors(), matcher);
    }

    private static Group getViewByA() {
        return getViewBy(String.valueOf(TAX_ID_A), TAX_LABEL_A, TAX_COUNT_A, true);
    }

    private static Group getViewByC() {
        return getViewBy(String.valueOf(TAX_ID_C), TAX_LABEL_C, TAX_COUNT_C, false);
    }

    private static Group getViewBy(String taxId, String taxLabel, long taxCount, boolean expand) {
        return MockServiceHelper.createViewBy(taxId, taxLabel, taxCount, expand);
    }

    private static Ancestor getAncestorA() {
        return getAncestor(String.valueOf(TAX_ID_A), TAX_LABEL_A);
    }

    private static Ancestor getAncestorB() {
        return getAncestor(String.valueOf(TAX_ID_B), TAX_LABEL_B);
    }

    private static Ancestor getAncestorD() {
        return getAncestor(String.valueOf(TAX_ID_D), TAX_LABEL_D);
    }

    private static Ancestor getAncestor(String id, String label) {
        return AncestorImpl.builder().id(id).label(label).build();
    }
}
