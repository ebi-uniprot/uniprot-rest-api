package uk.ac.ebi.uniprot.api.keyword;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.ac.ebi.uniprot.api.keyword.SnippetHelper.pathMatches;

/**
 * Created 22/07/19
 *
 * @author Edd
 */
class SnippetHelperTest {
    @Test
    void pathsMatchWhenPathVariableUsedAtEnd() {
        assertThat(pathMatches("/my/url/has/a/{pathParameter}", "/my/url/has/a/concreteValue"), is(true));
    }

    @Test
    void pathsMatchWhenMultiplePathVariableUsed() {
        assertThat(pathMatches("/my/url/{has}/{pathParameters}", "/my/url/concreteHas/concreteValue"), is(true));
    }

    @Test
    void pathsMatchWhenNoPathVariableUsed() {
        assertThat(pathMatches("/my/url/has/a/pathParameter", "/my/url/has/a/pathParameter"), is(true));
    }

    @Test
    void pathsMatchWhenPathVariableUsedInMiddle() {
        assertThat(pathMatches("/my/url/{has}/a/pathParameter", "/my/url/concreteHas/a/pathParameter"), is(true));
    }

    @Test
    void pathsMatchWhenPathVariableUsedAtStart() {
        assertThat(pathMatches("/{my}/url/has/a/pathParameter", "/concreteMy/url/has/a/pathParameter"), is(true));
    }
}