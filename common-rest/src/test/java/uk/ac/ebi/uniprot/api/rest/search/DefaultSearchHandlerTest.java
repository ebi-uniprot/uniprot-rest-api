package uk.ac.ebi.uniprot.api.rest.search;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.api.rest.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.api.rest.search.SearchField;
import uk.ac.ebi.uniprot.api.rest.search.SearchFieldType;
import uk.ac.ebi.uniprot.api.rest.validation.validator.FieldValueValidator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSearchHandlerTest {

    private final DefaultSearchHandler defaultSearchHandler = new DefaultSearchHandler(FakeSearchField.content,
            FakeSearchField.accession,FakeSearchField.getBoostFields());

    @Test
    void hasDefaultSearchSimpleTermReturnTrue(){
        String defaultQuery = "human";
        boolean hasDefaultSearch = defaultSearchHandler.hasDefaultSearch(defaultQuery);
        assertTrue(hasDefaultSearch);
    }

    @Test
    void hasDefaultSearchWithoutDefaultReturnFalse(){
        String defaultQuery = "organism:human";
        boolean hasDefaultSearch = defaultSearchHandler.hasDefaultSearch(defaultQuery);
        assertFalse(hasDefaultSearch);
    }

    @Test
    void hasDefaultSearchWithManyTermsReturnTrue(){
        String defaultQuery = "organism:human AND default";
        boolean hasDefaultSearch = defaultSearchHandler.hasDefaultSearch(defaultQuery);
        assertTrue(hasDefaultSearch);
    }

    @Test
    void hasDefaultSearchWithManyTermsValueReturnTrue(){
        String defaultQuery = "organism:homo sapiens";
        boolean hasDefaultSearch = defaultSearchHandler.hasDefaultSearch(defaultQuery);
        assertTrue(hasDefaultSearch);
    }

    @Test
    void rewriteDefaultSearchTermQuery() {
        String defaultQuery = "human";
        String result = defaultSearchHandler.optimiseDefaultSearch(defaultQuery);
        assertNotNull(result);
        assertEquals("content:human (taxonomy_name:human)^2.0 (gene:human)^2.0",result);
    }

    @Test
    void rewriteDefaultSearchMultipleTermQuery(){
        String defaultQuery = "P53 9606";
        String result = defaultSearchHandler.optimiseDefaultSearch(defaultQuery);
        assertNotNull(result);
        String expectedResult = "+(content:p53 (taxonomy_name:p53)^2.0 (gene:p53)^2.0) " +
                "+(content:9606 (taxonomy_name:9606)^2.0 (taxonomy_id:9606)^2.0 (gene:9606)^2.0)";
        assertEquals(expectedResult,result);
    }

    @Test
    void rewriteDefaultSearchWithValidIdTermQuery(){
        String defaultQuery = "P21802";
        String result = defaultSearchHandler.optimiseDefaultSearch(defaultQuery);
        assertNotNull(result);
        String expectedResult = "accession:p21802";
        assertEquals(expectedResult,result);
    }

    @Test
    void rewriteDefaultSearchWithOthersTermsQuery() {
        String defaultQuery = "organism:9606 human";
        String result = defaultSearchHandler.optimiseDefaultSearch(defaultQuery);
        assertNotNull(result);
        assertEquals("+organism:9606 +(content:human (taxonomy_name:human)^2.0 (gene:human)^2.0)",result);
    }

    @Test
    void rewriteDefaultSearchPharseQuery() {
        String defaultQuery = "\"homo sapiens\"";
        String result = defaultSearchHandler.optimiseDefaultSearch(defaultQuery);
        assertNotNull(result);
        String expectedResult = "content:\"homo sapiens\" (taxonomy_name:\"homo sapiens\")^2.0 (gene:\"homo sapiens\")^2.0";
        assertEquals(expectedResult,result);
    }

    @Test
    void rewriteDefaultSearchPharseQueryAndOthersTerms() {
        String defaultQuery = "organism:9606 \"homo sapiens\"";
        String result = defaultSearchHandler.optimiseDefaultSearch(defaultQuery);
        assertNotNull(result);
        String expectedResult = "+organism:9606 " +
                "+(content:\"homo sapiens\" (taxonomy_name:\"homo sapiens\")^2.0 (gene:\"homo sapiens\")^2.0)";
        assertEquals(expectedResult,result);
    }


    private enum FakeSearchField implements SearchField{
        content(SearchFieldType.TERM,null,null ),
        accession(SearchFieldType.TERM,FieldValueValidator::isAccessionValid,null ),
        taxonomy_name(SearchFieldType.TERM,null, 2.0f),
        taxonomy_id(SearchFieldType.TERM, FieldValueValidator::isNumberValue, 2.0f),
        gene(SearchFieldType.TERM,null,2.0f);

        private final Predicate<String> fieldValueValidator;
        private final SearchFieldType searchFieldType;
        private final Float boostValue;

        FakeSearchField(SearchFieldType searchFieldType,Predicate<String> fieldValueValidator,Float boostValue){
            this.searchFieldType = searchFieldType;
            this.fieldValueValidator  = fieldValueValidator;
            this.boostValue = boostValue;
        }

        private static List<SearchField> getBoostFields() {
            return Arrays.asList(FakeSearchField.taxonomy_name,FakeSearchField.taxonomy_id,FakeSearchField.gene);
        }

        @Override
        public boolean hasValidValue(String value) {
            return this.fieldValueValidator == null || this.fieldValueValidator.test(value);
        }

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public Float getBoostValue() {
            return this.boostValue;
        }
    }


}