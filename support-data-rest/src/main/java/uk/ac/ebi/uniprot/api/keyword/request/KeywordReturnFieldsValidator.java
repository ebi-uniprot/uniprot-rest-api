package uk.ac.ebi.uniprot.api.keyword.request;

import uk.ac.ebi.uniprot.search.field.KeywordField;
import uk.ac.ebi.uniprot.search.field.validator.ReturnFieldsValidator;

public class KeywordReturnFieldsValidator implements ReturnFieldsValidator {

    @Override
    public boolean hasValidReturnField(String fieldName) {
        boolean result = true;
        try {
            KeywordField.ResultFields.valueOf(fieldName);
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

}
