package org.uniprot.api.rest.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.store.config.UniProtDataType;

/**
 * Created 01/10/2019
 *
 * @author Edd
 */
class AbstractSolrSortClauseTest {
    private FakeSolrSortClause fakeSolrSortClause;

    @BeforeEach
    void beforeEach() {
        fakeSolrSortClause = new FakeSolrSortClause();
    }

    @Test
    void emptySortClauseProducesDefaultSort() {
        List<SolrQuery.SortClause> defaultSort = fakeSolrSortClause.getSort("");
        assertThat(
                defaultSort,
                contains(
                        SolrQuery.SortClause.desc("score"),
                        defaultSort(),
                        SolrQuery.SortClause.asc(FakeSolrSortClause.ID)));
    }

    @Test
    void singleSortClauseProducesSingleSort() {
        String name = "name";
        List<SolrQuery.SortClause> sorts = fakeSolrSortClause.getSort(name + " asc");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.asc(field(name)),
                        SolrQuery.SortClause.asc(FakeSolrSortClause.ID)));
    }

    @Test
    void invalidSortCausesException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> fakeSolrSortClause.getSort("invalid-no-sort-direction"));
    }

    @Test
    void multipleSortClausesWithIDProduceMultipleSorts() {
        String name = "name";
        String age = "age";
        List<SolrQuery.SortClause> sorts =
                fakeSolrSortClause.getSort(name + " asc, " + age + " desc, id desc");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.asc(field(name)),
                        SolrQuery.SortClause.desc(field(age)),
                        SolrQuery.SortClause.desc(FakeSolrSortClause.ID)));
    }

    @Test
    void multipleSortClausesProduceMultipleSorts() {
        String name = "name";
        String age = "age";
        List<SolrQuery.SortClause> sorts =
                fakeSolrSortClause.getSort(name + " asc, " + age + " desc");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.asc(field(name)),
                        SolrQuery.SortClause.desc(field(age)),
                        SolrQuery.SortClause.asc(FakeSolrSortClause.ID)));
    }

    private static class FakeSolrSortClause extends AbstractSolrSortClause {
        private static final String ID = "id_field";

        FakeSolrSortClause() {
            addDefaultFieldOrderPair("default", SolrQuery.ORDER.asc);
            addDefaultFieldOrderPair(FakeSolrSortClause.ID, SolrQuery.ORDER.asc);
        }

        @Override
        protected String getSolrDocumentIdFieldName() {
            return ID;
        }

        @Override
        public String getSolrSortFieldName(String name) {
            return field(name);
        }

        @Override
        protected UniProtDataType getUniProtDataType() {
            return null;
        }
    }

    static String field(String name) {
        return name + "_field";
    }

    static SolrQuery.SortClause defaultSort() {
        return SolrQuery.SortClause.asc("default");
    }
}
