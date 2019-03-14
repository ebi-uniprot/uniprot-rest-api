package uk.ac.ebi.uniprot.rest.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 *
 * @author lgonzales
 */
class SolrQueryUtilTest {
    @Test
    void hasFieldTermsWithValidTermsReturnTrue() {
        String inputQuery = "organism:human";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"organism");
        assertTrue(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithInvalidTermsReturnFalse() {
        String inputQuery = "organism:human";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"invalid");
        assertFalse(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithValidPhraseTermsReturnTrue() {
        String inputQuery = "organism:\"homo sapiens\"";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"organism");
        assertTrue(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithInvalidPhraseTermsReturnFalse() {
        String inputQuery = "organism:\"homo sapiens\"";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"invalid");
        assertFalse(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithValidRangeTermsReturnTrue() {
        String inputQuery = "length:[1 TO 10}";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"length");
        assertTrue(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithInvalidRangeTermsReturnFalse() {
        String inputQuery = "length:[1 TO 10}";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"invalid");
        assertFalse(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithValidBooleanQueryReturnTrue() {
        String inputQuery = "(organism:human) AND (length:[1 TO 10})";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"length","organism");
        assertTrue(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithInvalidBooleanQueryReturnFalse() {
        String inputQuery = "(organism:human) AND (length:[1 TO 10})";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"invalid2","invalid");
        assertFalse(hasFieldTerm);
    }

    @Test
    void hasFieldTermsWithOneValidBooleanQueryReturnTrue() {
        String inputQuery = "(organism:human) AND (length:[1 TO 10})";
        boolean hasFieldTerm = SolrQueryUtil.hasFieldTerms(inputQuery,"length","invalid");
        assertTrue(hasFieldTerm);
    }

}