package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.util.Utils;

@Constraint(validatedBy = ValidUniParcDatabaseList.UniParcDatabaseListValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUniParcDatabaseList {
    String message() default "invalid uniparc db name list";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UniParcDatabaseListValidator
            implements ConstraintValidator<ValidUniParcDatabaseList, String> {

        @Override
        public void initialize(ValidUniParcDatabaseList constraintAnnotation) {
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
            Optional<UniParcDatabase> upDB =
                    Arrays.stream(UniParcDatabase.values())
                            .filter(db -> dbName.equalsIgnoreCase(db.getDisplayName()))
                            .findAny();
            return upDB.isPresent();
        }

        void buildInvalidUniParcDBMessage(
                String dbName, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{uniparc.dbnames.invalid.dbname.message}";
            contextImpl.addMessageParameter("0", dbName);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
    }
}
