package org.uniprot.api.rest.validation;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;

class QuerySyntaxValidatorTest {
    @ParameterizedTest
    @CsvSource({
        "simple accession, accession:P21802-2, true",
        "boolean query, ((organism_id:9606) AND (gene:\"CDC7\")), true",
        "missing last bracket, ((organism_id:9606) AND (gene:\"CDC7\"), false",
        "missing closed double quote, ((organism_id:9606) AND (gene:\"CDC7)), false",
        "missing field colon ACTUALLY interpretted as two default query terms, ((organism_id 9606) AND (gene:CDC7)), true",
        "boolean query containing range, ((length:[1 TO 20]) AND (gene:CDC7)), true",
        "missing range closing bracket, ((length:[1 TO 20) AND (gene:CDC7)), false",
        "missing range upper bound, ((length:[1 TO ]) AND (gene:CDC7)), false",
        "query with forward slash, gene:MT1558/MT1560, true",
        "simple default query, protein, true",
        "default query with forward slash, a/b, true",
        "default query with multiple forward slashes, a/b/c, true",
        "default query with escaped slash and forward slash, a\\/b/c, true"
    })
    void checkValidationUseCase(String desc, String queryString, String expected) {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        assertThat(
                desc,
                validator.isValid(queryString, null),
                CoreMatchers.is(Boolean.valueOf(expected)));
    }

    @ParameterizedTest
    @CsvSource({
        "default query, a, a",
        "single forward slash, a/b, a\\/b",
        "single forward slash with numbers, 1/2, 1\\/2",
        "field query with single forward slash with letters, field:a/b, field:a\\/b",
        "field query with single forward slash with numbers, field:1/2, field:1\\/2",
        "separated forward slashes, a/b/c, a\\/b\\/c",
        "two adjacent forward slashes, a//b, a\\/\\/b",
        "contiguous forward slashes, a///b, a\\/\\/\\/b"
    })
    void checkForwardSlashReplacements(String desc, String queryString, String expected) {
        assertThat(
                desc,
                ValidSolrQuerySyntax.QuerySyntaxValidator.replaceForwardSlashes(queryString),
                CoreMatchers.is(expected));
    }
}
