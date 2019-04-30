package uk.ac.ebi.uniprot.api.proteome.request;


import uk.ac.ebi.uniprot.api.configure.proteome.ProteomeResultFields;
import uk.ac.ebi.uniprot.search.field.validator.ReturnFieldsValidator;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
*/

public class ProteomeReturnFieldsValidator implements ReturnFieldsValidator {

	  @Override
	    public boolean hasValidReturnField(String fieldName) {
	        return ProteomeResultFields.INSTANCE.getField(fieldName).isPresent();
	    }

}

