package org.uniprot.api.uniprotkb.groupby.service;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.groupby.model.Ancestor;
import org.uniprot.api.uniprotkb.groupby.model.AncestorImpl;
import org.uniprot.api.uniprotkb.groupby.model.Group;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.groupby.service.go.GOService;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoRelation;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.groupby.service.GroupByGOService.GO_PREFIX;

@ExtendWith(MockitoExtension.class)
class GroupByGOServiceTest {
    private static final String EMPTY_ID = "";
    private static final String GO_ID_A = "1425170";
    private static final String GO_ID_B = "9606";
    private static final String GO_ID_C = "21";
    private static final String GO_ID_D = "100";
    private static final String GO_ID_E = "59788";
    private static final String GO_ID_F = "13";
    private static final String GO_LABEL_A = "goLabelA";
    private static final String GO_LABEL_B = "goLabelB";
    private static final String GO_LABEL_C = "goLabelC";
    private static final String GO_LABEL_D = "goLabelD";
    private static final String GO_LABEL_E = "goLabelE";
    private static final String GO_LABEL_F = "goLabelF";
    private static final GoRelation GO_ENTRY_A = getGoRelation(GO_ID_A, GO_LABEL_A);
    private static final GoRelation GO_ENTRY_B = getGoRelation(GO_ID_B, GO_LABEL_B);
    private static final GoRelation GO_ENTRY_C = getGoRelation(GO_ID_C, GO_LABEL_C);
    private static final GoRelation GO_ENTRY_D = getGoRelation(GO_ID_D, GO_LABEL_D);
    private static final GoRelation GO_ENTRY_E = getGoRelation(GO_ID_E, GO_LABEL_E);
    private static final GoRelation GO_ENTRY_F = getGoRelation(GO_ID_F, GO_LABEL_F);
    private static final long GO_COUNT_A = 23L;
    private static final long GO_COUNT_B = 50L;
    private static final long GO_COUNT_C = 9999L;
    private static final long GO_COUNT_D = 10L;
    private static final long GO_COUNT_E = 12233L;
    private static final long GO_COUNT_F = 99L;
    private static final String SOME_NAME = "someName";
    private static final List<FacetField> SINGLE_GO_FACET_COUNTS_A =
            getFacetFields(GO_ID_A, GO_COUNT_A);
    private static final List<FacetField> SINGLE_GO_FACET_COUNTS_B =
            getFacetFields(GO_ID_B, GO_COUNT_B);
    private static final List<FacetField> SINGLE_GO_FACET_COUNTS_C =
            getFacetFields(GO_ID_C, GO_COUNT_C);
    private static final List<FacetField> SINGLE_GO_FACET_COUNTS_D =
            getFacetFields(GO_ID_D, GO_COUNT_D);
    private static final List<FacetField> SINGLE_GO_FACET_COUNTS_E =
            getFacetFields(GO_ID_E, GO_COUNT_E);
    private static final List<FacetField> MULTIPLE_GO_FACET_COUNTS = getMultipleFields();
    private static final String SOME_QUERY = "someQuery";
    @Mock private GOService goService;
    @Mock private UniProtEntryService uniProtEntryService;

    private GroupByGOService service;

    private static List<FacetField> getFacetFields(String id, long count) {
        return List.of(
                new FacetField(SOME_NAME) {
                    {
                        add(id, count);
                    }
                });
    }

    private static List<FacetField> getMultipleFields() {
        return List.of(
                new FacetField(SOME_NAME) {
                    {
                        add(GO_ID_A, GO_COUNT_A);
                        add(GO_ID_C, GO_COUNT_C);
                        add(GO_ID_F, GO_COUNT_F);
                    }
                });
    }

    @BeforeEach
    void setup() {
        service = new GroupByGOService(goService, uniProtEntryService);
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndMultipleRootNodes() {
        when(goService.getChildren(EMPTY_ID))
                .thenAnswer(invocation -> List.of(GO_ENTRY_A, GO_ENTRY_C, GO_ENTRY_F));
        when(goService.getChildren(
                        argThat(
                                arg ->
                                        arg != null
                                                && Set.of(
                                                                addGoPrefix(GO_ID_A),
                                                                addGoPrefix(GO_ID_F))
                                                        .contains(arg))))
                .thenAnswer(invocation -> List.of(GO_ENTRY_B));
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_C + "," + GO_ID_F)))
                .thenReturn(MULTIPLE_GO_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_B)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_B);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_ID);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithNoChildren() {
        when(goService.getChildren(EMPTY_ID)).thenAnswer(invocation -> List.of(GO_ENTRY_C));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_C)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_ID);

        assertViewByC(viewBys, empty());
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithMultipleChildren() {
        when(goService.getChildren(EMPTY_ID)).thenAnswer(invocation -> List.of(GO_ENTRY_B));
        when(goService.getChildren(addGoPrefix(GO_ID_B)))
                .thenAnswer(invocation -> List.of(GO_ENTRY_A, GO_ENTRY_C, GO_ENTRY_F));
        when(goService.getChildren(
                        argThat(
                                argument ->
                                        argument != null
                                                && Set.of(
                                                                addGoPrefix(GO_ID_A),
                                                                addGoPrefix(GO_ID_F))
                                                        .contains(argument))))
                .thenAnswer(invocation -> List.of(GO_ENTRY_E));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_B)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_C + "," + GO_ID_F)))
                .thenReturn(MULTIPLE_GO_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_E)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB()));
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilEdge() {
        when(goService.getChildren(any()))
                .thenAnswer(
                        invocation -> {
                            String parent = invocation.getArgument(0, String.class);
                            if (EMPTY_ID.equals(parent)) {
                                return List.of(GO_ENTRY_A);
                            }
                            if (addGoPrefix(GO_ID_A).equals(parent)) {
                                return List.of(GO_ENTRY_B);
                            }
                            if (addGoPrefix(GO_ID_B).equals(parent)) {
                                return List.of(GO_ENTRY_C);
                            }
                            return List.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_A)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_A);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_B)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_C)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_ID);

        assertViewByC(viewBys, contains(getAncestorA(), getAncestorB()));
    }

    @Test
    void
            getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilANodeWithMultipleChildren() {
        when(goService.getChildren(any()))
                .thenAnswer(
                        invocation -> {
                            String parent = invocation.getArgument(0, String.class);
                            if (EMPTY_ID.equals(parent)) {
                                return List.of(GO_ENTRY_B);
                            }
                            if (addGoPrefix(GO_ID_B).equals(parent)) {
                                return List.of(GO_ENTRY_D);
                            }
                            if (addGoPrefix(GO_ID_D).equals(parent)) {
                                return List.of(GO_ENTRY_A, GO_ENTRY_C, GO_ENTRY_F);
                            }
                            if (Set.of(addGoPrefix(GO_ID_A), addGoPrefix(GO_ID_F))
                                    .contains(parent)) {
                                return List.of(GO_ENTRY_E);
                            }
                            return List.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_B)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_D)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_C + "," + GO_ID_F)))
                .thenReturn(MULTIPLE_GO_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_E)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB(), getAncestorD()));
    }

    @Test
    void getGroupByResults_whenParentSpecifiedAndMultipleRootNodes() {
        when(goService.getChildren(argThat(argument -> addGoPrefix(GO_ID_B).equals(argument))))
                .thenAnswer(invocation -> List.of(GO_ENTRY_A, GO_ENTRY_C, GO_ENTRY_F));
        when(goService.getChildren(
                        argThat(
                                argument ->
                                        Set.of(addGoPrefix(GO_ID_A), addGoPrefix(GO_ID_F))
                                                .contains(argument))))
                .thenAnswer(invocation -> List.of(GO_ENTRY_E));
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_C + "," + GO_ID_F)))
                .thenReturn(MULTIPLE_GO_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_E)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, GO_ID_B);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getGroupByResults_whenParentSpecifiedAndNoChildNodes() {
        when(goService.getChildren(argThat(argument -> addGoPrefix(GO_ID_A).equals(argument))))
                .thenAnswer(invocation -> List.of());

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, GO_ID_A);

        assertThat(viewBys.getGroups(), empty());
        assertThat(viewBys.getAncestors(), empty());
    }

    @Test
    void getGroupByResults_whenParentSpecifiedAndSingleChildWithSingleChild_traverseUntilEnd() {
        when(goService.getChildren(any()))
                .thenAnswer(
                        invocation -> {
                            String parent = invocation.getArgument(0, String.class);
                            if (addGoPrefix(GO_ID_A).equals(parent)) {
                                return List.of(GO_ENTRY_B);
                            }
                            if (addGoPrefix(GO_ID_B).equals(parent)) {
                                return List.of(GO_ENTRY_C);
                            }
                            return List.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_B)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_C)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, GO_ID_A);

        assertViewByC(viewBys, contains(getAncestorB()));
    }

    @Test
    void
            getGroupByResults_whenParentSpecifiedAndSingleChildWithMultipleChildren_traverseUntilANodeWithMultipleChildren() {
        when(goService.getChildren(any()))
                .thenAnswer(
                        invocation -> {
                            String parent = invocation.getArgument(0, String.class);
                            if (addGoPrefix(GO_ID_B).equals(parent)) {
                                return List.of(GO_ENTRY_D);
                            }
                            if (addGoPrefix(GO_ID_D).equals(parent)) {
                                return List.of(GO_ENTRY_A, GO_ENTRY_C, GO_ENTRY_F);
                            }
                            if (Set.of(addGoPrefix(GO_ID_A), addGoPrefix(GO_ID_F))
                                    .contains(parent)) {
                                return List.of(GO_ENTRY_E);
                            }
                            return List.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_D)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_C + "," + GO_ID_F)))
                .thenReturn(MULTIPLE_GO_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_E)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_E);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, GO_ID_B);

        assertViewBysMultiple(viewBys, contains(getAncestorD()));
    }

    @Test
    void getGroupByResult_whenParentNotSpecifiedAndOnlyOneChildExistsInFacets() {
        when(goService.getChildren(any()))
                .thenAnswer(
                        invocation -> {
                            String parent = invocation.getArgument(0, String.class);
                            if (EMPTY_ID.equals(parent)) {
                                return List.of(GO_ENTRY_B);
                            }
                            if (addGoPrefix(GO_ID_B).equals(parent)) {
                                return List.of(GO_ENTRY_D);
                            }
                            if (addGoPrefix(GO_ID_D).equals(parent)) {
                                return List.of(GO_ENTRY_A, GO_ENTRY_E);
                            }
                            if (addGoPrefix(GO_ID_E).equals(parent)) {
                                return List.of(GO_ENTRY_C);
                            }
                            return List.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_B)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_D)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_E)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_E);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_C)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_ID);

        assertViewByC(viewBys, contains(getAncestorB(), getAncestorD(), getAncestorE()));
    }

    @Test
    void getGroupByResult_whenParentSpecifiedAndOnlyOneChildExistsInFacets() {
        when(goService.getChildren(any()))
                .thenAnswer(
                        invocation -> {
                            String parent = invocation.getArgument(0, String.class);
                            if (addGoPrefix(GO_ID_B).equals(parent)) {
                                return List.of(GO_ENTRY_D);
                            }
                            if (addGoPrefix(GO_ID_D).equals(parent)) {
                                return List.of(GO_ENTRY_A, GO_ENTRY_E);
                            }
                            if (addGoPrefix(GO_ID_E).equals(parent)) {
                                return List.of(GO_ENTRY_C);
                            }
                            return List.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_D)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                SOME_QUERY, getFacetFields(GO_ID_A + "," + GO_ID_E)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_E);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(GO_ID_C)))
                .thenReturn(SINGLE_GO_FACET_COUNTS_C);

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, GO_ID_B);

        assertViewByC(viewBys, contains(getAncestorD(), getAncestorE()));
    }

    private static String addGoPrefix(String goId) {
        return GO_PREFIX + goId;
    }

    private static Map<String, String> getFacetFields(String facetItems) {
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}go_id", facetItems));
    }

    private static GoRelation getGoRelation(String id, String label) {
        GoRelation goRelation = mock(GoRelation.class);
        when(goRelation.getId()).thenReturn(addGoPrefix(id));
        when(goRelation.getName()).thenReturn(label);
        return goRelation;
    }

    private static void assertViewBysMultiple(
            GroupByResult groupByResult, Matcher<? super List<Ancestor>> matcher) {
        assertThat(groupByResult.getGroups(), contains(getViewByA(), getViewByC(), getViewByF()));
        assertThat(groupByResult.getAncestors(), matcher);
    }

    private static void assertViewByC(
            GroupByResult viewBys, Matcher<? super List<Ancestor>> matcher) {
        assertThat(viewBys.getGroups(), contains(getViewByC()));
        assertThat(viewBys.getAncestors(), matcher);
    }

    private static Group getViewByA() {
        return getViewBy(GO_ID_A, GO_LABEL_A, GO_COUNT_A, true);
    }

    private static Group getViewByC() {
        return getViewBy(GO_ID_C, GO_LABEL_C, GO_COUNT_C, false);
    }

    private static Group getViewByF() {
        return getViewBy(GO_ID_F, GO_LABEL_F, GO_COUNT_F, true);
    }

    private static Group getViewBy(String goId, String goLabel, long goCount, boolean expand) {
        return MockServiceHelper.createViewBy(addGoPrefix(goId), goLabel, goCount, expand);
    }

    private static Ancestor getAncestorA() {
        return getAncestor(GO_ID_A, GO_LABEL_A);
    }

    private static Ancestor getAncestorB() {
        return getAncestor(GO_ID_B, GO_LABEL_B);
    }

    private static Ancestor getAncestorD() {
        return getAncestor(GO_ID_D, GO_LABEL_D);
    }

    private static Ancestor getAncestorE() {
        return getAncestor(GO_ID_E, GO_LABEL_E);
    }

    private static Ancestor getAncestor(String id, String label) {
        return AncestorImpl.builder().id(addGoPrefix(id)).label(label).build();
    }
}
