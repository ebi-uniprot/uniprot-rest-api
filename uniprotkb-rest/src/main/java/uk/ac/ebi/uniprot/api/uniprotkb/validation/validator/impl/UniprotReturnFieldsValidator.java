package uk.ac.ebi.uniprot.api.uniprotkb.validation.validator.impl;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.search.field.validator.ReturnFieldsValidator;

/**
 * This class is responsible to implement methods that are used to validate UNIPROT returned fields in the request
 * This is used in conjunction with @ValidReturnFields request validator
 *
 * @author lgonzales
 */
public class UniprotReturnFieldsValidator implements ReturnFieldsValidator{

    @Override
    public boolean hasValidReturnField(String fieldName) {
        return UniProtResultFields.INSTANCE.getField(fieldName).isPresent();
    }
}
