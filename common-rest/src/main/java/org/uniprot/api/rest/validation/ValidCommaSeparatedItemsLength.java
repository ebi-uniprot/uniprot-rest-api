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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.Utils;

@Constraint(validatedBy = ValidCommaSeparatedItemsLength.ListLengthValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCommaSeparatedItemsLength {
    String message() default "invalid comma separated list items count";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ListLengthValidator
            implements ConstraintValidator<ValidCommaSeparatedItemsLength, String> {

        @Value("${list.max.items.count}")
        private String maxLength;

        @Override
        public void initialize(ValidCommaSeparatedItemsLength constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;

                String[] values = value.split("\\s*,\\s*");
                int itemsCount = values.length;
                if (values.length > getMaxLength()) {
                    buildInvalidListLengthMessage(itemsCount, contextImpl);
                    isValid = false;
                }
                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }
            return isValid;
        }

        void buildInvalidListLengthMessage(
                int totalCount, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{list.invalid.max.items.count}";
            contextImpl.addMessageParameter("0", getMaxLength());
            contextImpl.addMessageParameter("1", totalCount);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        int getMaxLength() {
            return Integer.parseInt(maxLength);
        }
    }
}
