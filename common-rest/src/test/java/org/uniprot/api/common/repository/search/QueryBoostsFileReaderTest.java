package org.uniprot.api.common.repository.search;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
class QueryBoostsFileReaderTest {
    private QueryBoostsFileReader reader;

    @BeforeEach
    void setUp() {
        this.reader = new QueryBoostsFileReader("/test-boosts.config");
    }

    @Test
    void canLoadFile() {
        assertThat(reader, is(notNullValue()));
    }

    @Test
    void missingFileCausesException() {
        assertThrows(
                QueryBoostCreationException.class,
                () -> new QueryBoostsFileReader("this-file-does-not-exist"));
    }

    @Test
    void defaultBoostsLoadedCorrectly() {
        assertThat(
                reader.getQueryBoosts().getDefaultSearchBoosts(),
                containsInAnyOrder("default1:{query}^1.0", "default2:9606^2.0"));
    }

    @Test
    void defaultBoostFunctionsLoadedCorrectly() {
        assertThat(reader.getQueryBoosts().getDefaultSearchBoostFunctions(), is("default3"));
    }

    @Test
    void advancedBoostsLoadedCorrectly() {
        assertThat(reader.getQueryBoosts().getAdvancedSearchBoosts(), is(empty()));
    }

    @Test
    void advancedBoostFunctionsLoadedCorrectly() {
        assertThat(
                reader.getQueryBoosts().getAdvancedSearchBoostFunctions(),
                is("advanced1,advanced2"));
    }

    @Test
    void queryFieldsLoadedCorrectly() {
        assertThat(reader.getQueryBoosts().getQueryFields(), is("field1 field2"));
    }
}
