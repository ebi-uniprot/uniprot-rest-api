package uk.ac.ebi.uniprot.uuw.suggester.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion.ID_VALUE_SEPARATOR;

/**
 * Created 04/10/18
 *
 * @author Edd
 */
class SuggestionTest {
    @Test
    void givenPrefixNameAndId_whenBuildSuggestion_thenGetFormattedSuggestion() {
        String prefix = "prefix";
        String name = "name";
        String id = "id";
        Suggestion suggestion = Suggestion.builder()
                .prefix(prefix)
                .name(name)
                .id(id)
                .build();

        assertThat(suggestion.toSuggestionLine(), is(id + ID_VALUE_SEPARATOR + prefix + ": " + name));
    }

    @Test
    void givenPrefixAndName_whenBuildSuggestion_thenGetFormattedSuggestion() {
        String prefix = "prefix";
        String name = "name";
        Suggestion suggestion = Suggestion.builder()
                .prefix(prefix)
                .name(name)
                .build();

        assertThat(suggestion.toSuggestionLine(), is("@@ " + prefix + ": " + name));
    }

    @Test
    void givenNameAndId_whenBuildSuggestion_thenGetFormattedSuggestion() {
        String name = "name";
        String id = "id";
        Suggestion suggestion = Suggestion.builder()
                .name(name)
                .id(id)
                .build();

        assertThat(suggestion.toSuggestionLine(), is(id + ID_VALUE_SEPARATOR + name));
    }

    @Test
    void givenNullName_whenBuildSuggestion_thenGetException() {
        Suggestion suggestion = Suggestion.builder()
                .name(null)
                .build();

        Assertions.assertThrows(IllegalStateException.class, suggestion::toSuggestionLine);
    }

    @Test
    void givenEmptyName_whenBuildSuggestion_thenGetException() {
        Suggestion suggestion = Suggestion.builder()
                .name("")
                .build();

        Assertions.assertThrows(IllegalStateException.class, suggestion::toSuggestionLine);
    }

    @Test
    void givenWhitespaceName_whenBuildSuggestion_thenGetException() {
        Suggestion suggestion = Suggestion.builder()
                .name("  \t")
                .build();

        Assertions.assertThrows(IllegalStateException.class, suggestion::toSuggestionLine);
    }
}