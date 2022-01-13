package org.uniprot.api.rest.request;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.request.MetaReaderUtil.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 * @since 18/05/2020
 */
class ReturnFieldMetaReaderImplTest {

    @Test
    void readInvalidFile() {
        ReturnFieldMetaReaderImpl returnFieldMetaReaderImpl = new ReturnFieldMetaReaderImpl();
        assertThrows(
                IllegalArgumentException.class,
                () -> returnFieldMetaReaderImpl.read("invalid-invalid.json"));
    }

    @Test
    void readCrossReference() {
        ReturnFieldMetaReaderImpl returnFieldMetaReaderImpl = new ReturnFieldMetaReaderImpl();
        List<Map<String, Object>> result =
                returnFieldMetaReaderImpl.read("crossref-return-fields.json");
        assertNotNull(result);

        assertEquals(10, result.size());
        assertTrue(validateFieldMap(result, "name", "abbrev"));
        assertTrue(validateFieldMap(result, "label", "Database Abbreviation"));
    }

    @Test
    void readUniParcReturnFields() {
        ReturnFieldMetaReaderImpl returnFieldMetaReaderImpl = new ReturnFieldMetaReaderImpl();
        List<Map<String, Object>> result =
                returnFieldMetaReaderImpl.read("uniparc-return-fields.json");
        assertNotNull(result);

        assertEquals(24, result.size());
        assertTrue(validateFieldMap(result, "name", "sequence"));
        assertTrue(validateFieldMap(result, "label", "Sequence"));
    }
}
