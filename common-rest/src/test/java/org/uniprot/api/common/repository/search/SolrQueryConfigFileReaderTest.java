package org.uniprot.api.common.repository.search;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
class SolrQueryConfigFileReaderTest {
    private SolrQueryConfigFileReader reader;

    @BeforeEach
    void setUp() {
        this.reader = new SolrQueryConfigFileReader("/test-boosts.config");
    }

    @Test
    void canLoadFile() {
        assertThat(reader, is(notNullValue()));
    }

    @Test
    void missingFileCausesException() {
        assertThrows(
                SolrQueryConfigCreationException.class,
                () -> new SolrQueryConfigFileReader("this-file-does-not-exist"));
    }

    @Test
    void boostsLoadedCorrectly() {
        assertThat(reader.getConfig().getFieldBoosts(), containsInAnyOrder("default1:{query}^1.0"));
        assertThat(reader.getConfig().getStaticBoosts(), containsInAnyOrder("default2:9606^2.0"));
    }

    @Test
    void boostFunctionsLoadedCorrectly() {
        assertThat(reader.getConfig().getBoostFunctions(), is("default3"));
    }

    @Test
    void queryFieldsLoadedCorrectly() {
        assertThat(reader.getConfig().getQueryFields(), is("field1 field2"));
    }

    @Test
    void stopWordsLoadedCorrectly() {
        assertThat(reader.getConfig().getStopWords(), containsInAnyOrder("a", "which"));
    }

    @Test
    void highlightFieldsLoadedCorrectly() {
        assertThat(
                reader.getConfig().getHighlightFields(), is("highlight_field1,highlight_field2"));
    }

    @Test
    void checkEmptyBoostMapsInitialisedCorrected() {
        SolrQueryConfig queryConfig = SolrQueryConfig.builder().build();

        assertThat(queryConfig.getFieldBoosts().size(), is(0));
        assertThat(queryConfig.getStaticBoosts().size(), is(0));
    }

    @Test
    void checkBoostMapsInitialisedCorrectedWith1Value() {
        SolrQueryConfig queryConfig =
                SolrQueryConfig.builder()
                        .addBoost("field1:{query}^1.1")
                        .addBoost("field2:9606^2.0")
                        .build();

        List<String> fieldBoosts = queryConfig.getFieldBoosts();
        List<String> staticBoosts = queryConfig.getStaticBoosts();

        assertThat(fieldBoosts.size(), is(1));
        assertThat(staticBoosts.size(), is(1));

        assertThat(fieldBoosts, containsInAnyOrder("field1:{query}^1.1"));
        assertThat(staticBoosts, containsInAnyOrder("field2:9606^2.0"));
    }

    @Test
    void checkBoostMapsInitialisedCorrectedWith2Values() {
        SolrQueryConfig queryConfig =
                SolrQueryConfig.builder()
                        .addBoost("default1:{query}^1.1")
                        .addBoost("default2:{query}^4")
                        .addBoost("static1:9606^2.0")
                        .addBoost("static2:hello^4")
                        .build();

        List<String> fieldBoosts = queryConfig.getFieldBoosts();
        List<String> staticBoosts = queryConfig.getStaticBoosts();

        assertThat(fieldBoosts.size(), is(2));
        assertThat(staticBoosts.size(), is(2));

        assertThat(fieldBoosts, containsInAnyOrder("default1:{query}^1.1", "default2:{query}^4"));
        assertThat(staticBoosts, containsInAnyOrder("static1:9606^2.0", "static2:hello^4"));
    }
}
