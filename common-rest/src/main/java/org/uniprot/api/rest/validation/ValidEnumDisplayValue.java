package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.EnumDisplay;
import org.uniprot.core.util.Utils;

@Constraint(validatedBy = ValidEnumDisplayValue.EnumDisplayValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnumDisplayValue {
    String message() default "invalid Enum display values";

    Class<? extends EnumDisplay> enumDisplay();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EnumDisplayValidator implements ConstraintValidator<ValidEnumDisplayValue, String> {

        protected final List<String> acceptedValues = new ArrayList<>();

        @Override
        public void initialize(ValidEnumDisplayValue constraintAnnotation) {
            EnumDisplay[] values = constraintAnnotation.enumDisplay().getEnumConstants();
            Arrays.stream(values)
                    .map(EnumDisplay::getDisplayName)
                    .map(String::toLowerCase)
                    .forEach(acceptedValues::add);
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                // verify if db name is valid.
                String[] dbNames = value.split("\\s*,\\s*");
                for (String dbName : dbNames) {
                    if (!isValidUniParcDatabase(dbName)) {
                        buildInvalidUniParcDBMessage(dbName, contextImpl);
                        isValid = false;
                    }
                }

                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }
            return isValid;
        }

        private boolean isValidUniParcDatabase(String dbName) {
            return acceptedValues.contains(dbName.toLowerCase());
        }

        void buildInvalidUniParcDBMessage(
                String dbName, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{uniparc.dbnames.invalid.dbname.message}";
            contextImpl.addMessageParameter("0", dbName);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
    }
}
