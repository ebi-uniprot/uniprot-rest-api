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
import org.uniprot.api.uniprotkb.groupby.service.ec.ECService;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.cv.ec.ECEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.groupby.service.UniProtKBGroupByECService.*;

@ExtendWith(MockitoExtension.class)
class UniProtKBGroupECServiceTest {
    private static final String EMPTY_PARENT_ID = "";
    private static final long EC_COUNT_A = 23L;
    private static final long EC_COUNT_B = 50L;
    private static final long EC_COUNT_C = 9999L;
    private static final long EC_COUNT_D = 10L;
    private static final long EC_COUNT_E = 1995L;
    private static final String SOME_NAME = "someName";
    public static final String LABEL = "label";
    private static String ecIdA = "";
    private static String ecIdB = "";
    private static String ecIdC = "";
    private static String ecIdD = "";
    private static String ecIdE = "";
    private static final String SOME_QUERY = "someQuery";
    @Mock private ECService ecService;
    @Mock private UniProtEntryService uniProtEntryService;
    private UniProtKBGroupByECService service;

    @BeforeEach
    void setup() {
        service = new UniProtKBGroupByECService(ecService, uniProtEntryService);
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndMultipleRootNodes() {
        ecIdA = "1.-.-.-";
        ecIdB = "1.1.-.-";
        ecIdC = "2.-.-.-";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of())))
                .thenReturn(getMultipleFacetFields());
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getFacetFields("1.1", EC_COUNT_B));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithNoChildren() {
        ecIdC = "2.-.-.-";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of())))
                .thenReturn(getFacetFields(ecIdC, EC_COUNT_C));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys, empty());
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithMultipleChildren() {
        ecIdB = "1.-.-.-";
        ecIdA = "1.1.-.-";
        ecIdC = "1.2.-.-";
        ecIdE = "1.1.1.-";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of())))
                .thenReturn(getFacetFields(ecIdB, EC_COUNT_B));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getMultipleFacetFields());
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1"))))
                .thenReturn(getFacetFields(ecIdE, EC_COUNT_E));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB()));
    }

    @Test
    void getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilEdge() {
        ecIdA = "1.-.-.-";
        ecIdB = "1.1.-.-";
        ecIdC = "1.1.1.-";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of())))
                .thenReturn(getFacetFields(ecIdA, EC_COUNT_A));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getFacetFields(ecIdB, EC_COUNT_B));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1"))))
                .thenReturn(getFacetFields(ecIdC, EC_COUNT_C));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys, contains(getAncestorA(), getAncestorB()));
    }

    @Test
    void
            getGroupByResults_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilANodeWithMultipleChildren() {
        ecIdB = "1.-.-.-";
        ecIdD = "1.1.-.-";
        ecIdA = "1.1.1.-";
        ecIdC = "1.1.2.-";
        ecIdE = "1.1.1.1";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of())))
                .thenReturn(getFacetFields(ecIdB, EC_COUNT_B));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getFacetFields(ecIdD, EC_COUNT_D));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1"))))
                .thenReturn(getMultipleFacetFields());
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1", "1"))))
                .thenReturn(getFacetFields(ecIdE, EC_COUNT_E));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB(), getAncestorD()));
    }

    @Test
    void getGroupByResults_whenParentSpecifiedAndMultipleRootNodes() {
        ecIdB = "1.-.-.-";
        ecIdA = "1.1.-.-";
        ecIdC = "1.2.-.-";
        ecIdE = "1.1.1.-";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getMultipleFacetFields());
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1"))))
                .thenReturn(getFacetFields(ecIdE, EC_COUNT_E));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, ecIdB);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getGroupByResults_whenParentSpecifiedAndNoChildNodes() {
        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, ecIdA);

        assertThat(viewBys.getResults(), empty());
        assertThat(viewBys.getAncestors(), empty());
    }

    @Test
    void getGroupByResults_whenParentSpecifiedAndSingleChildWithSingleChild_traverseUntilEnd() {
        ecIdA = "1.-.-.-";
        ecIdB = "1.1.-.-";
        ecIdC = "1.1.1.-";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getFacetFields(ecIdB, EC_COUNT_B));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1"))))
                .thenReturn(getFacetFields(ecIdC, EC_COUNT_C));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, ecIdA);

        assertViewByC(viewBys, contains(getAncestorB()));
    }

    @Test
    void
            getGroupByResults_whenParentSpecifiedAndSingleChildWithMultipleChildren_traverseUntilANodeWithMultipleChildren() {
        ecIdB = "1.-.-.-";
        ecIdD = "1.1.-.-";
        ecIdA = "1.1.1.-";
        ecIdC = "1.1.2.-";
        ecIdE = "1.1.1.1";
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1"))))
                .thenReturn(getFacetFields(ecIdD, EC_COUNT_D));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1"))))
                .thenReturn(getMultipleFacetFields());
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(List.of("1", "1", "1"))))
                .thenReturn(getFacetFields(ecIdE, EC_COUNT_E));
        mockLabels();

        GroupByResult viewBys = service.getGroupByResult(SOME_QUERY, ecIdB);

        assertViewBysMultiple(viewBys, contains(getAncestorD()));
    }

    private List<FacetField> getFacetFields(String id, long count) {
        return List.of(
                new FacetField(SOME_NAME) {
                    {
                        add(id.split(".-")[0], count);
                    }
                });
    }

    private List<FacetField> getMultipleFacetFields() {
        return List.of(
                new FacetField(SOME_NAME) {
                    {
                        add(ecIdA.split(".-")[0], EC_COUNT_A);
                        add(ecIdC.split(".-")[0], EC_COUNT_C);
                    }
                });
    }

    private static Map<String, String> getFacetFields(List<String> entries) {
        String regEx =
                entries.stream()
                        .map(token -> token + TOKEN_REGEX)
                        .collect(Collectors.joining("", "", REGEX_SUFFIX));
        return Map.of(
                FacetParams.FACET_MATCHES,
                regEx,
                FacetParams.FACET_FIELD,
                EC,
                FacetParams.FACET_MINCOUNT,
                FACET_MIN_COUNT);
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
        return getViewBy(ecIdA, getLabel(ecIdA), EC_COUNT_A, true);
    }

    private static Group getViewByC() {
        return getViewBy(ecIdC, getLabel(ecIdC), EC_COUNT_C, false);
    }

    private static Group getViewBy(String ecId, String ecLabel, long ecCount, boolean expand) {
        return MockServiceHelper.createViewBy(ecId, ecLabel, ecCount, expand);
    }

    private static Ancestor getAncestorA() {
        return getAncestor(ecIdA, getLabel(ecIdA));
    }

    private static Ancestor getAncestorB() {
        return getAncestor(ecIdB, getLabel(ecIdB));
    }

    private static Ancestor getAncestorD() {
        return getAncestor(ecIdD, getLabel(ecIdD));
    }

    private static String getLabel(String suffix) {
        return LABEL + suffix;
    }

    private static Ancestor getAncestor(String id, String label) {
        return AncestorImpl.builder().id(id).label(label).build();
    }

    private void mockLabels() {
        when(ecService.getEC(any()))
                .thenAnswer(
                        invocation -> {
                            String fullEC = invocation.getArgument(0, String.class);
                            return Optional.of(
                                    new ECEntry() {
                                        @Override
                                        public String getId() {
                                            return fullEC;
                                        }

                                        @Override
                                        public String getLabel() {
                                            return LABEL + fullEC;
                                        }
                                    });
                        });
    }
}
