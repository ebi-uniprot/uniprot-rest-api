package org.uniprot.api.rest.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

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
        Sort defaultSort = fakeSolrSortClause.getSort("", false);
        List<String> defaultSorts =
                defaultSort.get().map(Sort.Order::toString).collect(Collectors.toList());
        assertThat(
                defaultSorts,
                contains(
                        defaultSort().toString(),
                        new Sort(Sort.Direction.ASC, FakeSolrSortClause.ID).toString()));
    }

    @Test
    void singleSortClauseProducesSingleSort() {
        String name = "name";
        Sort sort = fakeSolrSortClause.getSort(name + " asc", false);

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
                () -> fakeSolrSortClause.getSort("invalid-no-sort-direction", false));
    }

    @Test
    void multipleSortClausesWithIDProduceMultipleSorts() {
        String name = "name";
        String age = "age";
        Sort sort = fakeSolrSortClause.getSort(name + " asc, " + age + " desc, id desc", false);

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
        Sort sort = fakeSolrSortClause.getSort(name + " asc, " + age + " desc", false);

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

        @Override
        protected Sort createDefaultSort(boolean hasScore) {
            return defaultSort();
        }

        @Override
        protected String getSolrDocumentIdFieldName() {
            return ID;
        }

        @Override
        protected String getSolrSortFieldName(String name) {
            return field(name);
        }
    }

    static String field(String name) {
        return name + "_field";
    }

    static Sort defaultSort() {
        return new Sort(Sort.DEFAULT_DIRECTION, "default");
    }
}
