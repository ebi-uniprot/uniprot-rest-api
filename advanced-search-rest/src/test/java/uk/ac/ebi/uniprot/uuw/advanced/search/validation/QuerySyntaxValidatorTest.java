package uk.ac.ebi.uniprot.uuw.advanced.search.validation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuerySyntaxValidatorTest {

    @Test
    void isValidSimpleAccessionQueryReturnTrue() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("accession:P21802-2",null);
        assertEquals(true,result);
    }

    @Test
    void isValidBooleanAndQueryReturnTrue() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\"))",null);
        assertEquals(true,result);
    }


    @Test
    void isValidMissingBracketsReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\")",null);
        assertEquals(false,result);
    }

    @Test
    void isValidMissingOneDoubleQuoteReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7))",null);
        assertEquals(false,result);
    }

    @Test
    void isValidMissingParameterColonReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((organism_id 9606) AND (gene:\"CDC7))",null);
        assertEquals(false,result);
    }

    @Test
    void isValidMissingRangeBracketReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((length:[1 TO 20) AND (gene:\"CDC7))",null);
        assertEquals(false,result);
    }

    @Test
    void isValidMissingRangeEndValueReturnFalse() {
        ValidSolrQuerySyntax.QuerySyntaxValidator validator = new ValidSolrQuerySyntax.QuerySyntaxValidator();

        boolean result = validator.isValid("((length:[1 TO ]) AND (gene:\"CDC7))",null);
        assertEquals(false,result);
    }
}
