package uk.ac.ebi.uniprot.api.uniparc.request;

import uk.ac.ebi.uniprot.api.configure.uniparc.UniParcResultFields;
import uk.ac.ebi.uniprot.search.field.validator.ReturnFieldsValidator;

/**
 *
 * @author jluo
 * @date: 25 Jun 2019
 *
*/

public class UniParcReturnFieldsValidator implements ReturnFieldsValidator {

	@Override
    public boolean hasValidReturnField(String fieldName) {
        return UniParcResultFields.INSTANCE.getField(fieldName).isPresent();
    }


}

