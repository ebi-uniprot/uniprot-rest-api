package org.uniprot.api.rest.validation;

import static org.hamcrest.MatcherAssert.assertThat;

import javax.validation.ConstraintValidatorContext;

import org.hamcrest.CoreMatchers;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

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
        "default query with escaped slash and forward slash, a\\/b/c, true",
        "single middle wildcards, def*ault value, true",
        "multiple middle wildcards, def*aul*t value, false",
        "leading and trailing wildcard, *protein* value, true",
        "multiple leading, **defa*ult* value, true",
        "multiple trailing, *defaul*t** value, true",
        "multiple strip, **defaul*t** value, true"
    })
    void checkValidationUseCase(String desc, String queryString, String expected) {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        ConstraintValidatorContextImpl context = Mockito.mock(ConstraintValidatorContextImpl.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString()))
                .thenReturn(builder);

        assertThat(
                desc,
                validator.isValid(queryString, context),
                CoreMatchers.is(Boolean.valueOf(expected)));
    }
}
