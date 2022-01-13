package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@Constraint(validatedBy = IsEmpty.IsEmptyValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsEmpty {
    String message() default "this field must have empty value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class IsEmptyValidator implements ConstraintValidator<IsEmpty, String> {

        @Override
        public void initialize(IsEmpty constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && value.isEmpty();
        }
    }
}
