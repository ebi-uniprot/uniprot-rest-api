package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.ReturnField;

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

    Class<? extends Enum<? extends ReturnField>> fieldValidatorClazz();

    String message() default "{search.invalid.return.field}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class ReturnFieldsValidatorImpl implements ConstraintValidator<ValidReturnFields, String> {

        private List<ReturnField> returnFieldList;

        @Override
        public void initialize(ValidReturnFields constraintAnnotation) {
            try {
                returnFieldList = new ArrayList<>();
                Class<? extends Enum<? extends ReturnField>> enumClass =
                        constraintAnnotation.fieldValidatorClazz();

                Enum[] enumValArr = enumClass.getEnumConstants();

                for (Enum enumVal : enumValArr) {
                    returnFieldList.add((ReturnField) enumVal);
                }
            } catch (Exception e) {
                log.error("Error initializing ReturnFieldsValidator", e);
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullOrEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                String[] fieldList = value.split("\\s*,\\s*");
                for (String field : fieldList) {
                    if (!hasValidReturnField(field)) {
                        buildErrorMessage(field, contextImpl);
                        isValid = false;
                    }
                }
                if (!isValid) {
                    disableDefaultErrorMessage(contextImpl);
                }
            }
            return isValid;
        }

        public void disableDefaultErrorMessage(ConstraintValidatorContextImpl contextImpl) {
            contextImpl.disableDefaultConstraintViolation();
        }

        public void buildErrorMessage(String field, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.return.field}";
            contextImpl.addMessageParameter("0", field);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        private boolean hasValidReturnField(String fieldName) {
            return returnFieldList.stream()
                    .anyMatch(returnField -> returnField.hasReturnField(fieldName));
        }
    }
}
