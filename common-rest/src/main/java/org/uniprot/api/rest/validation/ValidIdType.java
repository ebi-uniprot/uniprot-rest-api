package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

/**
 * @author sahmad
 * @created 26/02/2021
 */
@Constraint(validatedBy = ValidIdType.ValidIdTypeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIdType {
    String message() default "invalid from/to type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class ValidIdTypeValidator implements ConstraintValidator<ValidIdType, String> {
        @Override
        public boolean isValid(String idType, ConstraintValidatorContext context) {
            boolean isValid = IdMappingFieldConfig.isValidDbName(idType);
            if (!isValid) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                contextImpl.addMessageParameter("0", idType);
            }
            return isValid;
        }
    }
}
