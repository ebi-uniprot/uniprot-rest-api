package uk.ac.ebi.uniprot.api.proteome.request;

import uk.ac.ebi.uniprot.search.field.GeneCentricField;
import uk.ac.ebi.uniprot.search.field.validator.ReturnFieldsValidator;

/**
 *
 * @author jluo
 * @date: 17 Jun 2019
 *
*/

public class GeneCentricReturnFieldsValidator implements ReturnFieldsValidator {

	@Override
	public boolean hasValidReturnField(String fieldName) {
		   boolean result = true;
	        try{
	            GeneCentricField.ResultFields.valueOf(fieldName);
	        }catch (Exception e){
	            result = false;
	        }
	        return result;
	}

}

