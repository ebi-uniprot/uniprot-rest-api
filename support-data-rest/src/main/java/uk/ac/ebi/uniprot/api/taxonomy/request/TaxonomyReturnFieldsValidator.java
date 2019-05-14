package uk.ac.ebi.uniprot.api.taxonomy.request;

import uk.ac.ebi.uniprot.search.field.TaxonomyField;
import uk.ac.ebi.uniprot.search.field.validator.ReturnFieldsValidator;

/**
 *
 * @author lgonzales
 */
public class TaxonomyReturnFieldsValidator implements ReturnFieldsValidator {

    @Override
    public boolean hasValidReturnField(String fieldName) {
        boolean result = true;
        try{
            TaxonomyField.ResultFields.valueOf(fieldName);
        }catch (Exception e){
            result = false;
        }
        return result;
    }
}
