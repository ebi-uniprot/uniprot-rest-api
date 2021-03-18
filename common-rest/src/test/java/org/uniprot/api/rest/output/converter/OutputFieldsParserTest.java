package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * @author lgonzales
 * @since 2020-04-03
 */
class OutputFieldsParserTest {

    @Test
    void canParseEmptyFieldsReturnDefaultSearch() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.KEYWORD);
        List<ReturnField> returnField = OutputFieldsParser.parse("", returnFieldConfig);

        assertNotNull(returnField);
        assertEquals(returnFieldConfig.getDefaultReturnFields(), returnField);
    }

    @Test
    void canParseFieldsReturnSelectedReturnedField() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.KEYWORD);
        List<ReturnField> returnField = OutputFieldsParser.parse("id,name", returnFieldConfig);

        assertNotNull(returnField);
        assertEquals(2, returnField.size());
        assertEquals("id", returnField.get(0).getName());
        assertEquals("name", returnField.get(1).getName());
    }

    @Test
    void canGetData() {

        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.KEYWORD);
        List<ReturnField> returnField =
                OutputFieldsParser.parse("id,name,definition", returnFieldConfig);

        Map<String, String> mappedFields = new HashMap<>();
        mappedFields.put("id", "idValue");
        mappedFields.put("name", "nameValue");
        List<String> result = OutputFieldsParser.getData(mappedFields, returnField);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("idValue", result.get(0));
        assertEquals("nameValue", result.get(1));
        assertEquals("", result.get(2));
    }
}
