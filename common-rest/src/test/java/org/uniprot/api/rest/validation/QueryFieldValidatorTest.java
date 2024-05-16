package org.uniprot.api.rest.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.validation.QueryFieldValidatorTest.FakeQueryFieldValidator.ErrorType;

import java.util.*;

import javax.validation.ConstraintValidatorContext;

import org.apache.lucene.search.Query;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.model.SearchFieldType;

/**
 * Unit Test class to validate QueryFieldValidator class behaviour
 *
 * @author lgonzales
 */
class QueryFieldValidatorTest {

    @Test
    void isValidDefaultSearchQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("P21802-2", null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleAccessionQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("accession:P21802-2", null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleLowerCaseAccessionQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("accession:p21802-2", null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleWildcardQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("gene:*", null);
        assertTrue(result);
    }

    @Test
    void isValidSimpleMiddleWildcardQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("ec:7.2.*.1", null);
        assertTrue(result);
    }

    @Test
    void isValidSimplePrefixQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("ec:7.2.*", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanAndQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("((organism_id:9606) AND (gene:\"CDC7\"))", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanOrQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("((organism_id:9606) OR (gene:\"CDC7\"))", null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanSubQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result =
                validator.isValid(
                        "((organism_id:9606) OR "
                                + "(gene:\"CDC7\") OR "
                                + "(cc_bpcp_kinetics:\"value\"))",
                        null);
        assertTrue(result);
    }

    @Test
    void isValidBooleanMultiSubQueryReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result =
                validator.isValid(
                        "(((organism_id:9606) OR (organism_id:1234)) AND "
                                + "(gene:\"CDC7\") AND "
                                + "(cc_bpcp_kinetics:1234))",
                        null);
        assertTrue(result);
    }

    @Test
    void isValidWhiteListFieldReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result =
                validator.isValid(
                        "HGNC:12345 AND PR:A0PK11 AND SLP:000001924 AND HostDB:ENSG00000182022 AND MetaCyc:HS04074-MON AND EcoCyc:PD00221",
                        null);
        assertTrue(result);
    }

    @Test
    void isValidUnsuportedBoostQueryTypeReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("organism_name:human^2", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.INVALID_TYPE).size());
        assertEquals(
                "org.apache.lucene.search.BoostQuery",
                validator.getErrorFields(ErrorType.INVALID_TYPE).get(0));
    }

    @Test
    void isValidInvalidOrganismNameRangeQueryFilterTypeReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("organism_name:[a TO z]", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.TYPE).size());
        assertEquals("organism_name", validator.getErrorFields(ErrorType.TYPE).get(0));
    }

    @Test
    void isValidInvalidFieldNameReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("invalid:P21802", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.FIELD).size());
        assertEquals("invalid", validator.getErrorFields(ErrorType.FIELD).get(0));
    }

    @Test
    void isValidInvalidAccessionReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("accession:P21802 OR " + "accession:invalidValue", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("accession", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidProteomeReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result =
                validator.isValid("proteome:UP123456789 OR " + "proteome:notProteomeId", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("proteome", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidBooleanFieldReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("active:notBoolean", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("active", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidInvalidIntegerFieldReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("taxonomy_id:notInteger", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.VALUE).size());
        assertEquals("taxonomy_id", validator.getErrorFields(ErrorType.VALUE).get(0));
    }

    @Test
    void isValidWithForwardSlashReturnTrue() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("a/b//c", null);
        assertTrue(result);
    }

    @Test
    void isValidInvalidFieldWithForwardSlashReturnFalse() {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid("invalid:a//b/c", null);
        assertFalse(result);
        assertEquals(1, validator.getErrorFields(ErrorType.FIELD).size());
        assertEquals("invalid", validator.getErrorFields(ErrorType.FIELD).get(0));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "amoebadb:ACA1_000210",
                "cryptodb:cgd6_2720",
                "ecocyc:G7409-MONOMER",
                "fb:FBgn0086655",
                "fungidb:NCU02389",
                "hostdb:ENSMFAG00000008940",
                "giardiadb:GL50581_3440",
                "hgnc:123456",
                "lncrna:CR42862",
                "metacyc:ENSG00000162971-MONOMER",
                "microsporidiadb:ECU07_0840",
                "mgi:1860512",
                "mrna:MD14G0162800",
                "pac:PPA1390",
                "phi:10479",
                "piroplasmadb:TA12905",
                "plasmodb:PF3D7_0703700",
                "pr:A0PK11",
                "refseq:YP_009725255",
                "refseq:P0DTD3",
                "rgd:1305291",
                "sgd:S000003801",
                "slp:000001973",
                "toxodb:TGME49_244440",
                "trichdb:TVAG_095560",
                "tritrypdb:LmjF.28.0550",
                "vectorbase:BGLB000032",
                "vgnc:11568",
                "wb:WBGene00012341",
                "xenbase:XB-GENE-491829",
                "zfin:ZDB-GENE-040426-2920"
            })
    void testValidWhitelistPrefixes(String xrefId) {
        ValidSolrQueryFields validSolrQueryFields = getMockedValidSolrQueryFields();
        FakeQueryFieldValidator validator = new FakeQueryFieldValidator();
        validator.initialize(validSolrQueryFields);

        boolean result = validator.isValid(xrefId, null);
        assertTrue(result);
    }

    private ValidSolrQueryFields getMockedValidSolrQueryFields() {
        ValidSolrQueryFields validSolrQueryFields = Mockito.mock(ValidSolrQueryFields.class);
        Mockito.when(validSolrQueryFields.uniProtDataType()).thenReturn(UniProtDataType.UNIPROTKB);
        return validSolrQueryFields;
    }

    /** this class is responsible to fake buildErrorMessage to help tests with */
    static class FakeQueryFieldValidator extends ValidSolrQueryFields.QueryFieldValidator {

        enum ErrorType {
            VALUE,
            TYPE,
            FIELD,
            INVALID_TYPE
        }

        FakeQueryFieldValidator() {
            Arrays.stream(ErrorType.values())
                    .forEach(errorType -> errorFields.put(errorType, new ArrayList<>()));
        }

        final Map<ErrorType, List<String>> errorFields = new HashMap<>();

        List<String> getErrorFields(ErrorType errorType) {
            return errorFields.get(errorType);
        }

        @Override
        public void addFieldValueErrorMessage(
                String fieldName, String value, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.VALUE).add(fieldName);
        }

        @Override
        public void addFieldTypeErrorMessage(
                String fieldName,
                SearchFieldType type,
                ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.TYPE).add(fieldName);
        }

        @Override
        public void addFieldNameErrorMessage(
                String fieldName, ConstraintValidatorContextImpl contextImpl) {
            errorFields.get(ErrorType.FIELD).add(fieldName);
        }

        @Override
        public void addQueryTypeErrorMessage(Query inputQuery, ConstraintValidatorContext context) {
            errorFields.get(ErrorType.INVALID_TYPE).add(inputQuery.getClass().getName());
        }

        @Override
        WhitelistFieldConfig getWhitelistFieldConfig() {
            WhitelistFieldConfig config;
            try {
                config = super.getWhitelistFieldConfig();
            } catch (Exception e) {
                config = new WhitelistFieldConfig();
            }
            Map<String, String> whiteListFields = new HashMap<>();
            whiteListFields.put("hgnc", "^[0-9]+$");
            whiteListFields.put(
                    "pr",
                    "(?i)([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?");
            whiteListFields.put("slp", "^[0-9]{9}$");
            whiteListFields.put("hostdb", "^[0-9A-Za-z-]+$");
            whiteListFields.put("ecocyc", "^[0-9A-Za-z-]+$");
            whiteListFields.put("metacyc", "^[0-9A-Za-z-]+$");
            whiteListFields.put("amoebadb", "^[0-9A-Za-z_]+$");
            whiteListFields.put("cryptodb", "^[0-9A-Za-z_]+$");
            whiteListFields.put("fb", "^[0-9A-Za-z]+$");
            whiteListFields.put("fungidb", "^[0-9A-Za-z_]+$");
            whiteListFields.put("giardiadb", "^[0-9A-Za-z_]+$");
            whiteListFields.put("lncrna", "^[0-9A-Za-z]+$");
            whiteListFields.put("mgi", "^[0-9]+");
            whiteListFields.put("microsporidiadb", "^[0-9A-Za-z_]+");
            whiteListFields.put("mrna", "^[0-9A-Za-z_]+");
            whiteListFields.put("pac", "^[0-9A-Z]+");
            whiteListFields.put("phi", "^[0-9]+");
            whiteListFields.put("piroplasmadb", "^[0-9A-Za-z_]+");
            whiteListFields.put("plasmodb", "^[0-9A-Za-z_]+");
            whiteListFields.put("refseq", "^[0-9A-Za-z_]+");
            whiteListFields.put("rgd", "^[0-9]+");
            whiteListFields.put("sgd", "^[0-9A-Z]+");
            whiteListFields.put("toxodb", "^[0-9A-Za-z_]+");
            whiteListFields.put("trichdb", "^[0-9A-Za-z_]+");
            whiteListFields.put("tritrypdb", "^[0-9A-Za-z\\.]+");
            whiteListFields.put("vectorbase", "^[0-9A-Z]+");
            whiteListFields.put("vgnc", "^[0-9]+");
            whiteListFields.put("wb", "^[0-9A-Za-z]+");
            whiteListFields.put("xenbase", "^[0-9A-Za-z-]+");
            whiteListFields.put("zfin", "^[0-9A-Za-z-]+");

            Map<String, Map<String, String>> whiteListCollection = new HashMap<>();
            whiteListCollection.put("uniprotkb", whiteListFields);
            config.setField(whiteListCollection);
            return config;
        }
    }
}
