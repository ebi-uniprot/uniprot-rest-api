package org.uniprot.api.rest.service.query;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import nl.altindag.log.LogCaptor;

/**
 * This class tests the full flow of functionality offered by {@link UniProtQueryProcessor},
 * including query String parsing by the lucene {@link StandardSyntaxParser}, different types of
 * queries and optimising fields.
 *
 * <p>Created 24/08/2020
 *
 * @author Edd
 */
@ExtendWith(MockitoExtension.class)
class UniProtQueryProcessorTest {
    private static final String FIELD_NAME = "accession";
    private UniProtQueryProcessor processor;
    private UniProtQueryProcessorConfig config;
    @Mock private SearchFieldConfig searchFieldConfig;

    @BeforeEach
    void setUp() {
        Map<String, String> whitelistFields = new HashMap<>();
        whitelistFields.put("go", "^[0-9]+$");
        config =
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(
                                singletonList(
                                        searchFieldWithValidRegex(FIELD_NAME, "(?i)^P[0-9]+$")))
                        .whiteListFields(whitelistFields)
                        .searchFieldConfig(searchFieldConfig)
                        .leadingWildcardFields(Set.of("gene", "protein_name"))
                        .build();
        processor = UniProtQueryProcessor.newInstance(config);
    }

    @Test
    void concurrentQueriesWithNewProcessorHasNoProblems() {
        LogCaptor logCaptor = LogCaptor.forClass(UniProtQueryProcessor.class);
        List<String> queries =
                List.of(
                        "(*)",
                        "this is a default query",
                        "(Methanococcoides burtonii Hel308)",
                        "(ethylsterigmatocystin oxidoreductase)",
                        "(L2EFL_DROME)",
                        "(L8I5P5)",
                        "(another type) AND (length:[10 TO 20])",
                        "Serine\\/alpha is okay");

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10000; i++) {
            executorService.submit(
                    () ->
                            UniProtQueryProcessor.newInstance(config)
                                    .processQuery(
                                            queries.get((int) (Math.random() * queries.size()))));
        }

        assertThat(logCaptor.getWarnLogs(), is(emptyList()));

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    @Test
    void concurrentQueriesWithSameProcessorHasProblems() {
        LogCaptor logCaptor = LogCaptor.forClass(UniProtQueryProcessor.class);
        List<String> queries =
                List.of(
                        "(*)",
                        "this is a default query",
                        "(Methanococcoides burtonii Hel308)",
                        "(ethylsterigmatocystin oxidoreductase)",
                        "(L2EFL_DROME)",
                        "(L8I5P5)",
                        "(another type) AND (length:[10 TO 20])",
                        "Serine\\/alpha is okay");

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10000; i++) {
            executorService.submit(
                    () ->
                            processor.processQuery(
                                    queries.get((int) (Math.random() * queries.size()))));
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        assertThat(logCaptor.getWarnLogs(), hasSize(greaterThan(0)));
        assertThat(
                logCaptor.getWarnLogs(),
                everyItem(Matchers.startsWith("Problem processing user query: ")));
    }

    @Test
    void optimiseWhitelistFieldQuery() {
        String processedQuery = processor.processQuery("GO:1234567");
        assertThat(processedQuery, is("GO\\:1234567"));
    }

    @Test
    void optimiseWhitelistFieldQueryValue() {
        String processedQuery = processor.processQuery("GO:1234567");
        assertThat(processedQuery, is("GO\\:1234567"));
    }

    @Test
    void optimiseWhitelistFieldNeedToBeTermQuery() {
        String processedQuery = processor.processQuery("GO AND 1234567");
        assertThat(processedQuery, is("GO AND 1234567"));
    }

    @Test
    void handleORQuery() {
        String processedQuery = processor.processQuery("a OR b");
        assertThat(processedQuery, is("a OR b"));
    }

    @Test
    void longerExample() {
        String processedQuery =
                processor.processQuery("The allele defined by Arg-6 and Glu-89 is associated ");
        assertThat(
                processedQuery,
                is(
                        "The AND allele AND defined AND by AND Arg-6 AND and AND Glu-89 AND is AND associated"));
    }

    @Test
    void useAndAsDefault() {
        String processedQuery = processor.processQuery("a b");
        assertThat(processedQuery, is("a AND b"));
    }

    @Test
    void useAndAsDefaultWithOneClauseAsAFieldQuery() {
        String processedQuery = processor.processQuery("a:thing b");
        assertThat(processedQuery, is("a:thing AND b"));
    }

    @Test
    void showPrecendenceWhileUsingAndAsDefault() {
        String processedQuery = processor.processQuery("a b OR c");
        assertThat(processedQuery, is("a AND ( b OR c )"));
    }

    @Test
    void showPrecendenceAgainWhileUsingAndAsDefault() {
        String processedQuery = processor.processQuery("a OR b c");
        assertThat(processedQuery, is("( a OR b ) AND c"));
    }

    @Test
    void showPrecendenceAgainWhileUsingAndAsDefault1() {
        String processedQuery = processor.processQuery("a:thing OR b c:thing");
        assertThat(processedQuery, is("( a:thing OR b ) AND c:thing"));
    }

    @Test
    void useAndAsDefaultInNestedQuery() {
        String processedQuery = processor.processQuery("a b (c (d OR e)) f");
        assertThat(processedQuery, is("a AND b AND ( c AND ( d OR e ) ) AND f"));
    }

    @Test
    void complexQueryWithNoOptimisation() {
        String query =
                "a OR ( b AND ( +c:something AND -d:something ) AND ( "
                        + "XX"
                        + " OR range:[1 TO 2] OR range:[1 TO *] OR range:[* TO 1] ) )";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handleUpperCaseFieldSearch() {
        String query = "FIELD:thing";
        when(searchFieldConfig.getSearchFieldNames()).thenReturn(Set.of("field"));
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is("field:thing"));
    }

    @Test
    void handleCamelCaseFieldSearch() {
        String query = "Field:thing_value";
        when(searchFieldConfig.getSearchFieldNames()).thenReturn(Set.of("field"));
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is("field:thing_value"));
    }

    @Test
    void handleUnderscoreDefaultSearch() {
        String query = "VAR_99999";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is("\"VAR_99999\""));
    }

    @Test
    void handleInvalidPrefixUnderscoreDefaultSearch() {
        String query = "9999_99999";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is("9999_99999"));
    }

    @Test
    void handleInvalidSuffixUnderScoreDefaultSearch() {
        String query = "VAR_VAR";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is("VAR_VAR"));
    }

    @Test
    void invalidQueryReturnsInSameQuery() throws QueryNodeException {
        UniProtQueryNodeProcessorPipeline mockPipeline =
                mock(UniProtQueryNodeProcessorPipeline.class);
        doThrow(new QueryNodeParseException(new RuntimeException()))
                .when(mockPipeline)
                .process(any(QueryNode.class));

        UniProtQueryProcessor processor = new UniProtQueryProcessor(mockPipeline, null);
        String query = "anything";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void queryProcessingErrorReturnsInSameQuery() throws QueryNodeException {
        UniProtQueryNodeProcessorPipeline mockPipeline =
                mock(UniProtQueryNodeProcessorPipeline.class);
        doThrow(new QueryNodeException(new RuntimeException()))
                .when(mockPipeline)
                .process(any(QueryNode.class));

        UniProtQueryProcessor processor = new UniProtQueryProcessor(mockPipeline, null);
        String query = "anything";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesFuzzySearch() {
        String query = "roam~3.0";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesFuzzyFieldSearch() {
        String query = "field:roam~3.0";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesProximitySearch() {
        String query = "\"hello world\"~3";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesFieldProximitySearch() {
        String query = "field:\"hello world\"~3";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesWildcardExpression() {
        String query = "*";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesFieldWildcardExpression() {
        String query = "a:*";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesLeadingWildcardExpression() {
        String query = "a:*thing";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is("a:thing"));
    }

    @Test
    void handlesTrailingWildcardExpression() {
        String query = "a:thing*";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesDefaultPhraseQuery() {
        String query = "\"this is an accession that is not optimised P12345\"";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    @Test
    void handlesFieldPhraseQuery() {
        String query = "field:\"this is an accession that is not optimised P12345\"";
        String processedQuery = processor.processQuery(query);
        assertThat(processedQuery, is(query));
    }

    private SearchFieldItem searchFieldWithValidRegex(String fieldName, String regex) {
        SearchFieldItem fieldItem = new SearchFieldItem();
        fieldItem.setFieldName(fieldName);
        fieldItem.setValidRegex(regex);
        return fieldItem;
    }

    @Nested
    class RangeQueries {
        @Test
        void inclusiveNumberRangeQueryHandled() {
            String query = "a:[1 TO 2]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void lessThanInclusiveRangeQueryHandled() {
            String query = "a:[* TO 2]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void lessThanNonInclusiveRangeQueryHandled() {
            String query = "a:{* TO 2}";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void greaterThanNonInclusiveRangeQueryHandled() {
            String query = "a:{2 TO *}";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void lessThanDateRangeQueryHandled() {
            String query = "a:[* TO NOW]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void greaterThanDateRangeQueryHandled() {
            String query = "a:[1988 TO NOW]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void complexDateRangeQueryHandled() {
            String query = "a:[NOW-7DAY\\/DAY TO NOW]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void dateRangeWithTimeQueryHandled() {
            String query = "a:[2013-07-17T00\\:00\\:00Z TO NOW]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }

        @Test
        void dateRangeWithTimeQueryAndStarHandled() {
            String query = "a:[2013-07-17T00\\:00\\:00Z TO *]";
            String processedQuery = processor.processQuery(query);
            assertThat(processedQuery, is(query));
        }
    }

    @Test
    void changeLowercaseAccessionToUppercase() {
        when(searchFieldConfig.getSearchFieldNames()).thenReturn(Set.of("accession"));

        String processedQuery = processor.processQuery("GO:1234567 OR accession:p12345");
        assertThat(processedQuery, is("GO\\:1234567 OR " + "accession:P12345"));
    }

    @Test
    void uppercaseAccessionUnchanged() {
        String processedQuery = processor.processQuery("GO:1234567 OR accession:P12345");
        assertThat(processedQuery, is("GO\\:1234567 OR " + "accession:P12345"));
    }

    @ParameterizedTest
    @MethodSource("getQueryWithLeadingWildcard")
    void testWithLeadingWildcardRemoved(String inputQuery, String expectedQuery) {
        String processedQuery = processor.processQuery(inputQuery);
        assertThat(processedQuery, is(expectedQuery));
    }

    private static Stream<Arguments> getQueryWithLeadingWildcard() {
        return Stream.of(
                Arguments.of("*quick brown fox", "quick AND brown AND fox"),
                Arguments.of("*quick brown* *fox", "quick AND brown* AND fox"),
                Arguments.of("*quick * brown fox", "quick AND * AND brown AND fox"),
                Arguments.of("*quick brown* ***fox", "quick AND brown* AND fox"),
                Arguments.of("otherfield:*quick brown fox", "otherfield:quick AND brown AND fox"),
                Arguments.of("*quick AND field:*text", "quick AND field:text"),
                Arguments.of(
                        "*quick brown fox AND field:*text",
                        "quick AND brown AND ( fox AND field:text )"),
                Arguments.of("*quick AND field:*text AND *brown", "quick AND field:text AND brown"),
                Arguments.of(
                        "*quick brown fox OR field:*text",
                        "quick AND brown AND ( fox OR field:text )"),
                Arguments.of("* AND field:*text*", "* AND field:text*"),
                Arguments.of("? AND field:?text?", "? AND field:text?"),
                Arguments.of("? AND field:???text?", "? AND field:text?"),
                Arguments.of("\"*quick brown fox\"", "\"*quick brown fox\""));
    }

    @ParameterizedTest
    @MethodSource("getQueryWithSupportedLeadingWildcard")
    void testWithLeadingWildcard(String inputQuery, String expectedQuery) {
        String processedQuery = processor.processQuery(inputQuery);
        assertThat(processedQuery, is(expectedQuery));
    }

    @Test
    void queryWithGreaterThanCharActualUserQuery() {
        String processedQuery = processor.processQuery("3'->5' exoribonuclease");
        assertThat(processedQuery, is("3'->5' AND exoribonuclease"));
    }

    @Test
    void queryWithGreaterThanChar() {
        String processedQuery = processor.processQuery("name>aaa exoribonuclease");
        assertThat(processedQuery, is("name>aaa AND exoribonuclease"));
    }

    @Test
    void queryWithLessThanChar() {
        String processedQuery = processor.processQuery("name<zzz exoribonuclease");
        assertThat(processedQuery, is("name<zzz AND exoribonuclease"));
    }

    @Test
    void queryWithGreaterThanCharInQuotedString() {
        String processedQuery = processor.processQuery("\"name>aaa exoribonuclease\"");
        assertThat(processedQuery, is("\"name>aaa exoribonuclease\""));
    }

    private static Stream<Arguments> getQueryWithSupportedLeadingWildcard() {
        return Stream.of(
                Arguments.of("gene:*CIROP AND field:*text", "gene:*CIROP AND field:text"),
                Arguments.of("protein_name:*CLRN2 AND *text", "protein_name:*CLRN2 AND text"),
                Arguments.of("protein_name:?CLRN2 AND ?text", "protein_name:?CLRN2 AND text"));
    }
}
