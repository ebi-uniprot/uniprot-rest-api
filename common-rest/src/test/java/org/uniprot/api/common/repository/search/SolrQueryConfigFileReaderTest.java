package org.uniprot.api.common.repository.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void defaultBoostsLoadedCorrectly() {
        assertThat(
                reader.getConfig().getDefaultSearchBoosts(),
                containsInAnyOrder("default1:{query}^1.0", "default2:9606^2.0"));
    }

    @Test
    void defaultBoostFunctionsLoadedCorrectly() {
        assertThat(reader.getConfig().getDefaultSearchBoostFunctions(), is("default3"));
    }

    @Test
    void advancedBoostsLoadedCorrectly() {
        assertThat(reader.getConfig().getAdvancedSearchBoosts(), is(empty()));
    }

    @Test
    void advancedBoostFunctionsLoadedCorrectly() {
        assertThat(reader.getConfig().getAdvancedSearchBoostFunctions(), is("advanced1,advanced2"));
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
        queryConfig.initialiseStaticAndFieldBoosts();

        assertThat(queryConfig.getFieldBoosts().size(), is(0));
    }

    @Test
    void checkBoostMapsInitialisedCorrectedWith1Value() {
        SolrQueryConfig queryConfig =
                SolrQueryConfig.builder()
                        .defaultSearchBoost("field1:{query}^1.1")
                        .defaultSearchBoost("field2:9606^2.0")
                        .build();
        queryConfig.initialiseStaticAndFieldBoosts();

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
                        .defaultSearchBoost("default1:{query}^1.1")
                        .defaultSearchBoost("default2:{query}^4")
                        .defaultSearchBoost("static1:9606^2.0")
                        .defaultSearchBoost("static2:hello^4")
                        .build();
        queryConfig.initialiseStaticAndFieldBoosts();

        List<String> fieldBoosts = queryConfig.getFieldBoosts();
        List<String> staticBoosts = queryConfig.getStaticBoosts();

        assertThat(fieldBoosts.size(), is(2));
        assertThat(staticBoosts.size(), is(2));

        assertThat(fieldBoosts, containsInAnyOrder("default1:{query}^1.1", "default2:{query}^4"));
        assertThat(staticBoosts, containsInAnyOrder("static1:9606^2.0", "static2:hello^4"));
    }
}
