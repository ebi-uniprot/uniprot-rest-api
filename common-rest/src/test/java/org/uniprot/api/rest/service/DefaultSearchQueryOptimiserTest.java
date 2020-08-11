package org.uniprot.api.rest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * @author lgonzales
 * @since 10/06/2020
 */
class DefaultSearchQueryOptimiserTest {

    @Test
    void testQueryOptimisationNotDefaultFieldSearchNoOptimisationDone() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "gene:queryValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals(query, optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchValidQueryOptimisation() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "validIdValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("idField:VALIDIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchValidQueryOptimisationAlternative() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "idField:validIdValue AND validIdValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("idField:validIdValue AND idField:VALIDIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchValidQueryOptimisation3() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "idField:validIdValue OR validIdValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("idField:validIdValue OR idField:VALIDIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithPlus() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "+validIdValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("+idField:VALIDIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithPlusAndBrackets() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "+idField:validIdValue +(validIdValue)";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("+idField:validIdValue +(idField:VALIDIDVALUE)", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithMinus() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "-validIdValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("-idField:VALIDIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithMinusAndBrackets() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "+idField:validIdValue -(validIdValue)";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("+idField:validIdValue -(idField:VALIDIDVALUE)", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithComplexBracketsAndPlusOutside() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "other (field:value +((validIdValue)))";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("other (field:value +((idField:VALIDIDVALUE)))", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithComplexBracketsAndPlusInside() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "other (field:value ((+validIdValue)))";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("other (field:value ((+idField:VALIDIDVALUE)))", optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchInvalidValueNoOptimisation() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validIdValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "InvalidValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals(query, optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldInBooleanQuerySearch() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "gene:queryValue AND (validValue OR otherValue)";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("gene:queryValue AND (idField:VALIDVALUE OR otherValue)", optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldInBooleanQuerySearchManyFields() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        fields.add(getSearchFieldItem("otherField", "otherValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "gene:queryValue AND (validValue OR otherValue OR ignoreValue)";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals(
                "gene:queryValue AND (idField:VALIDVALUE OR otherField:OTHERVALUE OR ignoreValue)",
                optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchWithDoubleQuotes() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "\"validValue\"";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("idField:VALIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchWithDoubleQuotesInBrackets() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "other (other2 \"validValue\")";
        //        String query = "(\"validValue\") (validValue thing (validValue~ OR X))";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("other (other2 idField:VALIDVALUE)", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWhenNextToLeftBracket() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "(validValue AND other)";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("(idField:VALIDVALUE AND other)", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWhenNextToRightBracket() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "(other AND validValue)";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("(other AND idField:VALIDVALUE)", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithRange() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "other OR value:[1 TO 10] OR validValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("other OR value:[1 TO 10] OR idField:VALIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithStarElseWhere() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "other* OR validValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("other* OR idField:VALIDVALUE", optimisedQuery);
    }

    @Test
    void testQueryOptimisationWithStarOnValue() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "other* OR validValue*";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("other* OR validValue*", optimisedQuery);
    }

    @Test
    void testNoQueryOptimisationWithValueImmediatelyAfterBrackets() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "(something AND other)validValue";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals("(something AND other)validValue", optimisedQuery);
    }

    @Test
    void testQueryWithMultipleOptimisations() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField1", "validValue1"));
        fields.add(getSearchFieldItem("idField2", "validValue2"));
        fields.add(getSearchFieldItem("idField3", "validValue3"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "(something AND other) AND validValue1 +(validValue2) AND ((validValue3))";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals(
                "(something AND other) AND idField1:VALIDVALUE1 +(idField2:VALIDVALUE2) AND ((idField3:VALIDVALUE3))",
                optimisedQuery);
    }

    @Test
    void testQueryOptimisationDefaultFieldSearchWithSingleQuotesDoNotOptimise() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "'validValue'";

        // when
        String optimisedQuery = searchQueryOptimiser.optimiseSearchQuery(query);

        // then
        assertNotNull(optimisedQuery);
        assertEquals(query, optimisedQuery);
    }

    protected SearchFieldItem getSearchFieldItem(String fieldName, String fieldValidValue) {
        SearchFieldItem fieldItem = new SearchFieldItem();
        fieldItem.setFieldName(fieldName);
        fieldItem.setValidRegex("^" + fieldValidValue + "$");
        return fieldItem;
    }
}
