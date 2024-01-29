package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.store.config.UniProtDataType;

@Constraint(validatedBy = ValidUniqueIdList.AccessionListValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUniqueIdList {
    String message() default "invalid id list";

    UniProtDataType uniProtDataType();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AccessionListValidator extends CommonIdsRequestValidator
            implements ConstraintValidator<ValidUniqueIdList, String> {

        @Value("${ids.max.length}")
        private String maxLength;

        private UniProtDataType dataType;

        @Autowired private HttpServletRequest request;

        @Override
        public void initialize(ValidUniqueIdList constraintAnnotation) {
            this.dataType = constraintAnnotation.uniProtDataType();
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid =
                    validateIdsAndPopulateErrorMessage(
                            value, getMaxLength(), getDataType(), context);
            if (!isValid && context != null) {
                context.disableDefaultConstraintViolation();
            }
            return isValid;
        }

        int getMaxLength() {
            return Integer.parseInt(maxLength);
        }

        UniProtDataType getDataType() {
            return this.dataType;
        }

        @Override
        HttpServletRequest getHttpServletRequest() {
            return this.request;
        }
    }
}
