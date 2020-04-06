package org.uniprot.api.rest.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
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
        Sort defaultSort = fakeSolrSortClause.getSort("");
        List<String> defaultSorts =
                defaultSort.get().map(Sort.Order::toString).collect(Collectors.toList());
        assertThat(
                defaultSorts,
                contains(
                        new Sort(Sort.Direction.DESC, "score").toString(),
                        defaultSort().toString(),
                        new Sort(Sort.Direction.ASC, FakeSolrSortClause.ID).toString()));
    }

    @Test
    void singleSortClauseProducesSingleSort() {
        String name = "name";
        Sort sort = fakeSolrSortClause.getSort(name + " asc");

        List<String> sorts = sort.get().map(Sort.Order::toString).collect(Collectors.toList());
        assertThat(
                sorts,
                contains(
                        new Sort(Sort.Direction.ASC, field(name)).toString(),
                        new Sort(Sort.Direction.ASC, FakeSolrSortClause.ID).toString()));
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
        Sort sort = fakeSolrSortClause.getSort(name + " asc, " + age + " desc, id desc");

        List<String> sorts = sort.get().map(Sort.Order::toString).collect(Collectors.toList());
        assertThat(
                sorts,
                contains(
                        new Sort(Sort.Direction.ASC, field(name)).toString(),
                        new Sort(Sort.Direction.DESC, field(age)).toString(),
                        new Sort(Sort.Direction.DESC, FakeSolrSortClause.ID).toString()));
    }

    @Test
    void multipleSortClausesProduceMultipleSorts() {
        String name = "name";
        String age = "age";
        Sort sort = fakeSolrSortClause.getSort(name + " asc, " + age + " desc");

        List<String> sorts = sort.get().map(Sort.Order::toString).collect(Collectors.toList());
        assertThat(
                sorts,
                contains(
                        new Sort(Sort.Direction.ASC, field(name)).toString(),
                        new Sort(Sort.Direction.DESC, field(age)).toString(),
                        new Sort(Sort.Direction.ASC, FakeSolrSortClause.ID).toString()));
    }

    private static class FakeSolrSortClause extends AbstractSolrSortClause {
        private static final String ID = "id_field";

        FakeSolrSortClause() {
            addDefaultFieldOrderPair("default", Sort.Direction.ASC);
            addDefaultFieldOrderPair(FakeSolrSortClause.ID, Sort.Direction.ASC);
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

    static Sort defaultSort() {
        return new Sort(Sort.DEFAULT_DIRECTION, "default");
    }
}
