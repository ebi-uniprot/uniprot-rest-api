package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static uk.ac.ebi.uniprot.uuw.advanced.search.results.TupleStreamTemplate.TupleStreamBuilder.fieldsToReturn;
import static uk.ac.ebi.uniprot.uuw.advanced.search.results.TupleStreamTemplate.TupleStreamBuilder.sortToString;

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
        assertThat(fieldsToReturn(key, order), is(Stream.of(key, field1).collect(Collectors.joining(","))));
    }

    @Test
    void givenFieldAndMultipleSort_whenFindFieldsToReturn_thenGetCorrectResults() {
        String key = "key";
        String field1 = "field1";
        String field2 = "field2";
        Sort order = new Sort(ASC, field1).and(new Sort(DESC, field2));
        assertThat(fieldsToReturn(key, order), is(Stream.of(key, field1, field2).collect(Collectors.joining(","))));
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
        assertThat(sortToString(order), is(Stream.of(field1 + " asc", field2 + " desc")
                                                   .collect(Collectors.joining(","))));
    }
}