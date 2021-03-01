package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
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
        private List<UniProtDatabaseDetail> idMappingDBNames;

        @Override
        public void initialize(ValidIdType constraintAnnotation) {
            this.idMappingDBNames = IdMappingFieldConfig.getAllIdMappingTypes();
        }

        @Override
        public boolean isValid(
                String idType, ConstraintValidatorContext constraintValidatorContext) {
            return this.idMappingDBNames.stream()
                    .anyMatch(type -> type.getIdMappingName().equals(idType));
        }
    }
}
