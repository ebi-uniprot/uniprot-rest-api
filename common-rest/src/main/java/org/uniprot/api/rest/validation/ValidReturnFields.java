package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * This Return Fields Constraint Validator class is responsible to verify if the inputted return
 * fields parameter have valid field names.
 *
 * <p>It return one message for each invalid field name.
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidReturnFields.ReturnFieldsValidatorImpl.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidReturnFields {

    UniProtDataType uniProtDataType();

    String message() default "{search.invalid.return.field}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class ReturnFieldsValidatorImpl extends CommonIdsRequestValidator
            implements ConstraintValidator<ValidReturnFields, String> {

        private ReturnFieldConfig returnFieldConfig;

        @Override
        public void initialize(ValidReturnFields constraintAnnotation) {
            try {
                UniProtDataType uniProtDataType = constraintAnnotation.uniProtDataType();
                returnFieldConfig = ReturnFieldConfigFactory.getReturnFieldConfig(uniProtDataType);
            } catch (Exception e) {
                log.error("Error initializing ReturnFieldsValidator", e);
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = isValidReturnFields(value, this.returnFieldConfig, context);
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            if (!isValid) {
                disableDefaultErrorMessage(contextImpl);
            }
            return isValid;
        }

        public void disableDefaultErrorMessage(ConstraintValidatorContextImpl contextImpl) {
            contextImpl.disableDefaultConstraintViolation();
        }
    }
}
