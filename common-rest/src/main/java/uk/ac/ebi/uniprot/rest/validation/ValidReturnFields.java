package uk.ac.ebi.uniprot.rest.validation;


import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import uk.ac.ebi.uniprot.rest.validation.validator.ReturnFieldsValidator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Return Fields Constraint Validator class is responsible to verify
 * if the inputted return fields parameter have valid field names.
 *
 * It return one message for each invalid field name.
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidReturnFields.ReturnFieldsValidatorImpl.class)
@Target( { ElementType.METHOD, ElementType.FIELD,ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidReturnFields {

    Class<? extends ReturnFieldsValidator> fieldValidatorClazz();

    String message() default "{search.invalid.return.field}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    public class ReturnFieldsValidatorImpl implements ConstraintValidator<ValidReturnFields, String> {

        public ReturnFieldsValidator fieldValidator = null;

        @Override
        public void initialize(ValidReturnFields constraintAnnotation) {
            try {
                fieldValidator = constraintAnnotation.fieldValidatorClazz().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if(value != null && !value.isEmpty()){
                ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
                String fieldList[] = value.split("\\s*,\\s*");
                for (String field : fieldList) {
                    if(!fieldValidator.hasValidReturnField(field)){
                        buildErrorMessage(field,contextImpl);
                        isValid = false;
                    }
                }
                if(!isValid){
                    disableDefaultErrorMessage(contextImpl);
                }
            }
            return isValid;
        }

        public void disableDefaultErrorMessage(ConstraintValidatorContextImpl contextImpl) {
            contextImpl.disableDefaultConstraintViolation();
        }

        public void buildErrorMessage(String field,ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.return.field}";
            contextImpl.addMessageParameter("fieldName",field);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
    }
}
