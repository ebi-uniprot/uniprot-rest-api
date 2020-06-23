package org.uniprot.api.common.repository.store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.uniprot.api.common.repository.store.TupleStreamTemplate.TupleStreamBuilder.fieldsToReturn;
import static org.uniprot.api.common.repository.store.TupleStreamTemplate.TupleStreamBuilder.sortToString;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;

/**
 * Created 23/10/18
 *
 * @author Edd
 */
class TupleStreamTemplateTest {

    @Test
    void givenFieldAndSingleSort_whenFindFieldsToReturn_thenGetCorrectResults() {
        String key = "key";
        String field1 = "field1";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        assertThat(fieldsToReturn(key, order), is(String.join(",", key, field1)));
    }

    @Test
    void givenFieldAndMultipleSort_whenFindFieldsToReturn_thenGetCorrectResults() {
        String key = "key";
        String field1 = "field1";
        String field2 = "field2";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        order.add(new SolrQuery.SortClause(field2, SolrQuery.ORDER.desc));
        assertThat(fieldsToReturn(key, order), is(String.join(",", key, field1, field2)));
    }

    @Test
    void givenSingleSort_whenSortToString_thenGetCorrectResults() {
        String field1 = "field1";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        assertThat(sortToString(order), is(field1 + " asc"));
    }

    @Test
    void givenMultipleSort_whenSortToString_thenGetCorrectResults() {
        String field1 = "field1";
        String field2 = "field2";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        order.add(new SolrQuery.SortClause(field2, SolrQuery.ORDER.desc));
        assertThat(sortToString(order), is(String.join(",", field1 + " asc", field2 + " desc")));
    }
}
