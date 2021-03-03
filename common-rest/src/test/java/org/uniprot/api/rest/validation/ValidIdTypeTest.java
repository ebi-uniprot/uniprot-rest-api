package org.uniprot.api.rest.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

/**
 * Created 26/02/2021
 *
 * @author sahmad
 */
class ValidIdTypeTest {
    private static ValidIdType.ValidIdTypeValidator validator;

    @BeforeAll
    static void setUp() {
        validator = new ValidIdType.ValidIdTypeValidator();
        validator.initialize(null);
    }

    @Test
    void testValidValue() {
        String value = IdMappingFieldConfig.convertDisplayNameToName("UniProtKB");
        Assertions.assertTrue(validator.isValid(value, null));
    }

    @Test
    void testInvalidValue() {
        String value = "invalid";
        Assertions.assertFalse(validator.isValid(value, null));
    }
}
