package org.uniprot.api.rest.validation;

import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ValidAccessionList.AccessionListValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccessionList {
    String message() default "invalid accession list";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AccessionListValidator implements ConstraintValidator<ValidAccessionList, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            int count = 0;
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                // verify if accession is valid.
                String[] accessions = value.split(",");
                count = accessions.length;
                for (String accession : accessions) {
                    isValid = accession.matches(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);
                }
            }
            if(count > 1000){
                isValid = false;
            }
            return isValid;
        }
    }
}
