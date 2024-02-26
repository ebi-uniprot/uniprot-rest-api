package org.uniprot.api.rest.service.query.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@ExtendWith(MockitoExtension.class)
class UniProtFieldQueryNodeProcessorTest {
    @Mock private SearchFieldConfig searchFieldConfig;

    @Before
    public void setUp() throws Exception {
        when(searchFieldConfig.findSearchFieldItemByAlias(anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void processInvalidFieldNameUpperCaseThenDoNothing() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldsNames(Set.of("field"))
                        .searchFieldConfig(searchFieldConfig)
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode("OTHER", "value", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("OTHER:value", result);
    }

    @Test
    void processValidFieldNameUpperCaseThenChangeItToLowerCase() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode("FIELD", "value", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("field:value", result);
    }

    @Test
    void processValidFieldNameMultipleCaseThenChangeItToLowerCase() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode("Field", "value", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("field:value", result);
    }

    @Test
    void toQueryString_validAlias_returnsFieldName() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldConfig(searchFieldConfig)
                        .searchFieldsNames(Set.of("otherField"))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);
        SearchFieldItem searchFieldItem = new SearchFieldItem();
        String fieldName = "field";
        searchFieldItem.setFieldName(fieldName);
        String alias = "alias";
        when(searchFieldConfig.findSearchFieldItemByAlias(alias))
                .thenReturn(Optional.of(searchFieldItem));

        FieldQueryNode node = new FieldQueryNode(alias, "value", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("field:value", result);
    }

    @Test
    void processDefaultSearchWithUnderScoreThenRemoveUnderScoreAndDoubleQuote()
            throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "VAR_99999", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("\"VAR_99999\"", result);
    }

    @Test
    void processDefaultSearchWithUnderScoreThenRemoveUnderScoreAndDoubleQuoteWithVSF()
            throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "VSF_99999", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("\"VSF_99999\"", result);
    }

    @Test
    void processDefaultSearchWithUnderScoreInvalidPrefix() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "99999_9999", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("99999_9999", result);
    }

    @Test
    void processDefaultSearchWithUnderScoreInvalidSufix() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "VAR_VAR", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("VAR_VAR", result);
    }

    @Test
    void processDefaultSearchWithNoOpitimisableWhiteListValue() throws QueryNodeException {
        SearchFieldItem optimisableField = new SearchFieldItem();
        optimisableField.setFieldName("field");
        optimisableField.setValidRegex("^[0-9]*");
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldsNames(Set.of("field"))
                        .optimisableFields(List.of(optimisableField))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "letter", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("letter", result);
    }

    @Test
    void processAccessionThenUpperCaseValue() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode("accession", "p21802", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("accession:P21802", result);
    }

    @Test
    void processWhiteListInvalidValue() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldsNames(Set.of("field"))
                        .searchFieldConfig(searchFieldConfig)
                        .whiteListFields(Map.of("slp", "^[0-9]{3}$"))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode("SLP", "1234", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("SLP:1234", result);
    }

    @Test
    void processWhiteListThenScapeColon() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldsNames(Set.of("field"))
                        .whiteListFields(Map.of("slp", "^[0-9]{3}$"))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode("SLP", "123", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("SLP\\:123", result);
    }

    @ParameterizedTest(name = "[\"{0}\" is \"{1}\"]")
    @MethodSource("getWildcardSearchQuery")
    void processDefaultSearchWithLeadingWildcard(String inputQuery, String expectedQuery)
            throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, inputQuery, 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals(expectedQuery, result);
    }

    private static Stream<Arguments> getWildcardSearchQuery() {
        return Stream.of(
                Arguments.of("*quick brown fox", "quick brown fox"),
                Arguments.of("**quick brown fox", "quick brown fox"),
                Arguments.of("*quick brown fox*", "quick brown fox*"),
                Arguments.of("*quick * brown fox*", "quick * brown fox*"),
                Arguments.of("*p12345", "p12345"),
                Arguments.of("\"*quick brown fox\"", "\"*quick brown fox\""));
    }

    @ParameterizedTest(name = "[\"{0}:{1}\" is \"{2}\"]")
    @MethodSource("getWildcardSearchQueryWithField")
    void processFieldSearchWithLeadingWildcard(
            String fieldName, String inputQuery, String expectedQuery) throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldsNames(Set.of("field"))
                        .searchFieldConfig(searchFieldConfig)
                        .whiteListFields(Map.of("slp", "^[0-9]{3}$"))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(fieldName, inputQuery, 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals(expectedQuery, result);
    }

    private static Stream<Arguments> getWildcardSearchQueryWithField() {
        return Stream.of(
                Arguments.of("other_field", "*quick brown fox", "other_field:quick\\ brown\\ fox"),
                Arguments.of(
                        UniProtQueryProcessor.UNIPROTKB_ACCESSION_FIELD,
                        "*p12345",
                        UniProtQueryProcessor.UNIPROTKB_ACCESSION_FIELD + ":P12345"),
                Arguments.of("SLP", "*123", "SLP\\:123"),
                Arguments.of("FIELD", "*random search text", "field:random search text"));
    }

    @ParameterizedTest
    @MethodSource("getQueryWithSupportedLeadingWildcard")
    void testWithLeadingWildcard(String fieldName, String inputQuery, String expectedQuery)
            throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .leadingWildcardFields(Set.of("gene", "protein_name"))
                        .searchFieldsNames(Set.of("field"))
                        .searchFieldConfig(searchFieldConfig)
                        .whiteListFields(Map.of("slp", "^[0-9]{3}$"))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(fieldName, inputQuery, 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals(expectedQuery, result);
    }

    private static Stream<Arguments> getQueryWithSupportedLeadingWildcard() {
        return Stream.of(
                Arguments.of("gene", "*CIROP", "gene:*CIROP"),
                Arguments.of("protein_name", "*CLRN2", "protein_name:*CLRN2"));
    }
}
