package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QuerySyntaxValidatorTest {

    @Test
    void isValidSimpleAccessionQueryReturnTrue() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("accession:P21802-2", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanAndQueryReturnTrue() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\"))", null);
        assertTrue(result);
    }

    @Test
    void isValidMissingBracketsReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\")", null);
        assertFalse(result);
    }

    @Test
    void isValidMissingOneDoubleQuoteReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7))", null);
        assertFalse(result);
    }

    @Test
    void isValidMissingParameterColonReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id 9606) AND (gene:\"CDC7))", null);
        assertFalse(result);
    }

    @Test
    void isValidMissingRangeBracketReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((length:[1 TO 20) AND (gene:\"CDC7))", null);
        assertFalse(result);
    }

    @Test
    void isValidMissingRangeEndValueReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((length:[1 TO ]) AND (gene:\"CDC7))", null);
        assertFalse(result);
    }

    @Test
    void isValidWithScapedSpecialChars() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("gene:MT1558\\/MT1560", null);
        assertTrue(result);
    }

    @Test
    void isValidWildcardQueryReturnTrue() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator =
                new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("accession:*", null);
        assertTrue(result);
    }
}
