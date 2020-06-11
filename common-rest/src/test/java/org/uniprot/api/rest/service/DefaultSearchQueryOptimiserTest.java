package org.uniprot.api.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.ServiceException;
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

    @Test
    void testQueryOptimisationInvalidQuerySearchThrowsException() {
        // given
        List<SearchFieldItem> fields = new ArrayList<>();
        fields.add(getSearchFieldItem("idField", "validValue"));
        DefaultSearchQueryOptimiser searchQueryOptimiser = new DefaultSearchQueryOptimiser(fields);
        String query = "gene:[queryValue ]";

        // then
        assertThrows(ServiceException.class, () -> searchQueryOptimiser.optimiseSearchQuery(query));
    }

    protected SearchFieldItem getSearchFieldItem(String fieldName, String fieldValidValue) {
        SearchFieldItem fieldItem = new SearchFieldItem();
        fieldItem.setFieldName(fieldName);
        fieldItem.setValidRegex("^" + fieldValidValue + "$");
        return fieldItem;
    }
}
