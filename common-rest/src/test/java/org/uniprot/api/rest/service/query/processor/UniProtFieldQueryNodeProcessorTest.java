package org.uniprot.api.rest.service.query.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.junit.jupiter.api.Test;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

class UniProtFieldQueryNodeProcessorTest {

    @Test
    void processInvalidFieldNameUpperCaseThenDoNothing() throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
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
    void processDefaultSearchWithUnderScoreThenRemoveUnderScoreAndDoubleQuote()
            throws QueryNodeException {
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder().searchFieldsNames(Set.of("field")).build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "VAR_99999", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("\"VAR 99999\"", result);
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
        assertEquals("\"VSF 99999\"", result);
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
    void processDefaultSearchWithOpitimisableWhiteListThenAddFieldName() throws QueryNodeException {
        SearchFieldItem optimisableField = new SearchFieldItem();
        optimisableField.setFieldName("field");
        optimisableField.setValidRegex("^[0-9]*");
        UniProtQueryProcessorConfig conf =
                UniProtQueryProcessorConfig.builder()
                        .searchFieldsNames(Set.of("field"))
                        .optimisableFields(List.of(optimisableField))
                        .build();
        UniProtFieldQueryNodeProcessor processor = new UniProtFieldQueryNodeProcessor(conf);

        FieldQueryNode node = new FieldQueryNode(IMPOSSIBLE_FIELD, "1245", 1, 2);
        QueryNode processedNode = processor.process(node);
        CharSequence result = processedNode.toQueryString(new EscapeQuerySyntaxImpl());
        assertNotNull(result);
        assertEquals("field:1245", result);
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
}
