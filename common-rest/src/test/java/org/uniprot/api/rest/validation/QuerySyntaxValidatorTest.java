package org.uniprot.api.rest.validation;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
