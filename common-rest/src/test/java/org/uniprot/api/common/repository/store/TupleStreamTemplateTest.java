package org.uniprot.api.common.repository.store;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.uniprot.api.common.repository.store.TupleStreamTemplate.TupleStreamBuilder.fieldsToReturn;
import static org.uniprot.api.common.repository.store.TupleStreamTemplate.TupleStreamBuilder.sortToString;

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
        Sort order = new Sort(ASC, field1);
        assertThat(fieldsToReturn(key, order), is(String.join(",", key, field1)));
    }

    @Test
    void givenFieldAndMultipleSort_whenFindFieldsToReturn_thenGetCorrectResults() {
        String key = "key";
        String field1 = "field1";
        String field2 = "field2";
        Sort order = new Sort(ASC, field1).and(new Sort(DESC, field2));
        assertThat(fieldsToReturn(key, order), is(String.join(",", key, field1, field2)));
    }

    @Test
    void givenSingleSort_whenSortToString_thenGetCorrectResults() {
        String field1 = "field1";
        Sort order = new Sort(ASC, field1);
        assertThat(sortToString(order), is(field1 + " asc"));
    }

    @Test
    void givenMultipleSort_whenSortToString_thenGetCorrectResults() {
        String field1 = "field1";
        String field2 = "field2";
        Sort order = new Sort(ASC, field1).and(new Sort(DESC, field2));
        assertThat(sortToString(order), is(String.join(",", field1 + " asc", field2 + " desc")));
    }
}