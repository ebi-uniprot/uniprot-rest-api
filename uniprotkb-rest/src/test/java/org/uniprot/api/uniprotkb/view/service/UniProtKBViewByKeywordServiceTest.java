package org.uniprot.api.uniprotkb.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.view.service.UniProtKBViewByKeywordService.TOP_LEVEL_PARENT_QUERY;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.uniprot.api.rest.service.keyword.KeywordService;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.api.uniprotkb.view.Ancestor;
import org.uniprot.api.uniprotkb.view.AncestorImpl;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByResult;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.keyword.KeywordId;

@ExtendWith(MockitoExtension.class)
class UniProtKBViewByKeywordServiceTest {
    private static final String EMPTY_PARENT_ID = "";
    private static final String KEYWORD_ID_A = "1425170";
    private static final String KEYWORD_ID_B = "9606";
    private static final String KEYWORD_ID_C = "21";
    private static final String KEYWORD_ID_D = "100";
    private static final String KEYWORD_ID_E = "59788";
    private static final String KEYWORD_ID_F = "13";
    public static final String PARENT_KEYWORD_ID_A = ("parent:" + KEYWORD_ID_A);
    public static final String PARENT_KEYWORD_ID_B = ("parent:" + KEYWORD_ID_B);
    public static final String PARENT_KEYWORD_ID_D = ("parent:" + KEYWORD_ID_D);
    public static final String PARENT_KEYWORD_ID_F = ("parent:" + KEYWORD_ID_F);
    private static final String KEYWORD_LABEL_A = "keywordLabelA";
    private static final String KEYWORD_LABEL_B = "keywordLabelB";
    private static final String KEYWORD_LABEL_C = "keywordLabelC";
    private static final String KEYWORD_LABEL_D = "keywordLabelD";
    private static final String KEYWORD_LABEL_E = "keywordLabelE";
    private static final String KEYWORD_LABEL_F = "keywordLabelF";
    private static final KeywordEntry KEYWORD_ENTRY_A =
            getKeywordEntry(KEYWORD_ID_A, KEYWORD_LABEL_A);
    private static final KeywordEntry KEYWORD_ENTRY_B =
            getKeywordEntry(KEYWORD_ID_B, KEYWORD_LABEL_B);
    private static final KeywordEntry KEYWORD_ENTRY_C =
            getKeywordEntry(KEYWORD_ID_C, KEYWORD_LABEL_C);
    private static final KeywordEntry KEYWORD_ENTRY_D =
            getKeywordEntry(KEYWORD_ID_D, KEYWORD_LABEL_D);
    private static final KeywordEntry KEYWORD_ENTRY_E =
            getKeywordEntry(KEYWORD_ID_E, KEYWORD_LABEL_E);
    private static final KeywordEntry KEYWORD_ENTRY_F =
            getKeywordEntry(KEYWORD_ID_F, KEYWORD_LABEL_F);
    private static final long KEYWORD_COUNT_A = 23L;
    private static final long KEYWORD_COUNT_B = 50L;
    private static final long KEYWORD_COUNT_C = 9999L;
    private static final long KEYWORD_COUNT_D = 10L;
    private static final long KEYWORD_COUNT_E = 12233L;
    private static final long KEYWORD_COUNT_F = 99L;
    private static final String SOME_NAME = "someName";
    private static final List<FacetField> SINGLE_KEYWORD_FACET_COUNTS_A =
            getFacetFields(KEYWORD_ID_A, KEYWORD_COUNT_A);
    private static final List<FacetField> SINGLE_KEYWORD_FACET_COUNTS_B =
            getFacetFields(KEYWORD_ID_B, KEYWORD_COUNT_B);
    private static final List<FacetField> SINGLE_KEYWORD_FACET_COUNTS_C =
            getFacetFields(KEYWORD_ID_C, KEYWORD_COUNT_C);
    private static final List<FacetField> SINGLE_KEYWORD_FACET_COUNTS_D =
            getFacetFields(KEYWORD_ID_D, KEYWORD_COUNT_D);
    private static final List<FacetField> SINGLE_KEYWORD_FACET_COUNTS_E =
            getFacetFields(KEYWORD_ID_E, KEYWORD_COUNT_E);
    private static final List<FacetField> MULTIPLE_KEYWORD_FACET_COUNTS = getMultipleFields();
    private static final String SOME_QUERY = "someQuery";
    @Mock private KeywordService keywordService;
    @Mock private UniProtEntryService uniProtEntryService;

    private UniProtKBViewByKeywordService service;

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
                        add(KEYWORD_ID_A, KEYWORD_COUNT_A);
                        add(KEYWORD_ID_C, KEYWORD_COUNT_C);
                        add(KEYWORD_ID_F, KEYWORD_COUNT_F);
                    }
                });
    }

    @BeforeEach
    void setup() {
        service = new UniProtKBViewByKeywordService(keywordService, uniProtEntryService);
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndMultipleRootNodes() {
        when(keywordService.stream(
                        argThat(
                                arg ->
                                        arg != null
                                                && TOP_LEVEL_PARENT_QUERY.equals(arg.getQuery()))))
                .thenAnswer(
                        invocation -> Stream.of(KEYWORD_ENTRY_A, KEYWORD_ENTRY_C, KEYWORD_ENTRY_F));
        when(keywordService.stream(
                        argThat(
                                arg ->
                                        arg != null
                                                && Set.of(PARENT_KEYWORD_ID_A, PARENT_KEYWORD_ID_F)
                                                        .contains(arg.getQuery()))))
                .thenAnswer(invocation -> Stream.of(KEYWORD_ENTRY_B));
        when(uniProtEntryService.getFacets(
                        SOME_QUERY,
                        getFacetFields(KEYWORD_ID_A + "," + KEYWORD_ID_C + "," + KEYWORD_ID_F)))
                .thenReturn(MULTIPLE_KEYWORD_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_B)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_B);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithNoChildren() {
        when(keywordService.stream(
                        argThat(argument -> (TOP_LEVEL_PARENT_QUERY).equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(KEYWORD_ENTRY_C));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_C)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_C);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys, empty());
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithMultipleChildren() {
        when(keywordService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && (TOP_LEVEL_PARENT_QUERY)
                                                        .equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(KEYWORD_ENTRY_B));
        when(keywordService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && PARENT_KEYWORD_ID_B.equals(
                                                        argument.getQuery()))))
                .thenAnswer(
                        invocation -> Stream.of(KEYWORD_ENTRY_A, KEYWORD_ENTRY_C, KEYWORD_ENTRY_F));
        when(keywordService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && Set.of(PARENT_KEYWORD_ID_A, PARENT_KEYWORD_ID_F)
                                                        .contains(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(KEYWORD_ENTRY_E));
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_B)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY,
                        getFacetFields(KEYWORD_ID_A + "," + KEYWORD_ID_C + "," + KEYWORD_ID_F)))
                .thenReturn(MULTIPLE_KEYWORD_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_E)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_E);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB()));
    }

    @Test
    void getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilEdge() {
        when(keywordService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (TOP_LEVEL_PARENT_QUERY.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_A);
                            }
                            if (PARENT_KEYWORD_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_B);
                            }
                            if (PARENT_KEYWORD_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_C);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_A)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_A);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_B)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_C)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_C);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewByC(viewBys, contains(getAncestorA(), getAncestorB()));
    }

    @Test
    void
            getViewBys_whenNoParentSpecifiedAndSingleRootNodeWithSingleChild_traverseUntilANodeWithMultipleChildren() {
        when(keywordService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (TOP_LEVEL_PARENT_QUERY.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_B);
                            }
                            if (PARENT_KEYWORD_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_D);
                            }
                            if (PARENT_KEYWORD_ID_D.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_A, KEYWORD_ENTRY_C, KEYWORD_ENTRY_F);
                            }
                            if (Set.of(PARENT_KEYWORD_ID_A, PARENT_KEYWORD_ID_F)
                                    .contains(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_E);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_B)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_D)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY,
                        getFacetFields(KEYWORD_ID_A + "," + KEYWORD_ID_C + "," + KEYWORD_ID_F)))
                .thenReturn(MULTIPLE_KEYWORD_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_E)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_E);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, EMPTY_PARENT_ID);

        assertViewBysMultiple(viewBys, contains(getAncestorB(), getAncestorD()));
    }

    @Test
    void getViewBys_whenParentSpecifiedAndMultipleRootNodes() {
        when(keywordService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && PARENT_KEYWORD_ID_B.equals(
                                                        argument.getQuery()))))
                .thenAnswer(
                        invocation -> Stream.of(KEYWORD_ENTRY_A, KEYWORD_ENTRY_C, KEYWORD_ENTRY_F));
        when(keywordService.stream(
                        argThat(
                                argument ->
                                        argument != null
                                                && Set.of(PARENT_KEYWORD_ID_A, PARENT_KEYWORD_ID_F)
                                                        .contains(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of(KEYWORD_ENTRY_E));
        when(uniProtEntryService.getFacets(
                        SOME_QUERY,
                        getFacetFields(KEYWORD_ID_A + "," + KEYWORD_ID_C + "," + KEYWORD_ID_F)))
                .thenReturn(MULTIPLE_KEYWORD_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_E)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_E);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, KEYWORD_ID_B);

        assertViewBysMultiple(viewBys, empty());
    }

    @Test
    void getViewBys_whenParentSpecifiedAndNoChildNodes() {
        when(keywordService.stream(
                        argThat(argument -> PARENT_KEYWORD_ID_A.equals(argument.getQuery()))))
                .thenAnswer(invocation -> Stream.of());

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, KEYWORD_ID_A);

        assertThat(viewBys.getResults(), empty());
        assertThat(viewBys.getAncestors(), empty());
    }

    @Test
    void getViewBys_whenParentSpecifiedAndSingleChildWithSingleChild_traverseUntilEnd() {
        when(keywordService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (PARENT_KEYWORD_ID_A.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_B);
                            }
                            if (PARENT_KEYWORD_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_C);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_B)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_B);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_C)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_C);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, KEYWORD_ID_A);

        assertViewByC(viewBys, contains(getAncestorB()));
    }

    @Test
    void
            getViewBys_whenParentSpecifiedAndSingleChildWithMultipleChildren_traverseUntilANodeWithMultipleChildren() {
        when(keywordService.stream(any()))
                .thenAnswer(
                        invocation -> {
                            StreamRequest streamRequest =
                                    invocation.getArgument(0, StreamRequest.class);
                            if (PARENT_KEYWORD_ID_B.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_D);
                            }
                            if (PARENT_KEYWORD_ID_D.equals(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_A, KEYWORD_ENTRY_C, KEYWORD_ENTRY_F);
                            }
                            if (Set.of(PARENT_KEYWORD_ID_A, PARENT_KEYWORD_ID_F)
                                    .contains(streamRequest.getQuery())) {
                                return Stream.of(KEYWORD_ENTRY_E);
                            }
                            return Stream.of();
                        });
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_D)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_D);
        when(uniProtEntryService.getFacets(
                        SOME_QUERY,
                        getFacetFields(KEYWORD_ID_A + "," + KEYWORD_ID_C + "," + KEYWORD_ID_F)))
                .thenReturn(MULTIPLE_KEYWORD_FACET_COUNTS);
        when(uniProtEntryService.getFacets(SOME_QUERY, getFacetFields(KEYWORD_ID_E)))
                .thenReturn(SINGLE_KEYWORD_FACET_COUNTS_E);

        ViewByResult viewBys = service.getViewBys(SOME_QUERY, KEYWORD_ID_B);

        assertViewBysMultiple(viewBys, contains(getAncestorD()));
    }

    private static Map<String, String> getFacetFields(String facetItems) {
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}keyword", facetItems));
    }

    private static KeywordEntry getKeywordEntry(String id, String label) {
        KeywordId keyword = mock(KeywordId.class);
        when(keyword.getName()).thenReturn(label);
        KeywordEntry keywordEntry = mock(KeywordEntry.class);
        when(keywordEntry.getAccession()).thenReturn(id);
        when(keywordEntry.getKeyword()).thenReturn(keyword);
        return keywordEntry;
    }

    private static void assertViewBysMultiple(
            ViewByResult viewByResult, Matcher<? super List<Ancestor>> matcher) {
        assertThat(viewByResult.getResults(), contains(getViewByA(), getViewByC(), getViewByF()));
        assertThat(viewByResult.getAncestors(), matcher);
    }

    private static void assertViewByC(
            ViewByResult viewBys, Matcher<? super List<Ancestor>> matcher) {
        assertThat(viewBys.getResults(), contains(getViewByC()));
        assertThat(viewBys.getAncestors(), matcher);
    }

    private static ViewBy getViewByA() {
        return getViewBy(KEYWORD_ID_A, KEYWORD_LABEL_A, KEYWORD_COUNT_A, true);
    }

    private static ViewBy getViewByC() {
        return getViewBy(KEYWORD_ID_C, KEYWORD_LABEL_C, KEYWORD_COUNT_C, false);
    }

    private static ViewBy getViewByF() {
        return getViewBy(KEYWORD_ID_F, KEYWORD_LABEL_F, KEYWORD_COUNT_F, true);
    }

    private static ViewBy getViewBy(
            String keywordId, String keywordLabel, long keywordCount, boolean expand) {
        return MockServiceHelper.createViewBy(keywordId, keywordLabel, keywordCount, expand);
    }

    private static Ancestor getAncestorA() {
        return getAncestor(KEYWORD_ID_A, KEYWORD_LABEL_A);
    }

    private static Ancestor getAncestorB() {
        return getAncestor(KEYWORD_ID_B, KEYWORD_LABEL_B);
    }

    private static Ancestor getAncestorD() {
        return getAncestor(KEYWORD_ID_D, KEYWORD_LABEL_D);
    }

    private static Ancestor getAncestor(String id, String label) {
        return AncestorImpl.builder().id(id).label(label).build();
    }
}
