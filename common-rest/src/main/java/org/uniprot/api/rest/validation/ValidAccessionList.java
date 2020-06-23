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
import org.uniprot.store.search.field.validator.FieldRegexConstants;

@Constraint(validatedBy = ValidAccessionList.AccessionListValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccessionList {
    String message() default "invalid accession list";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AccessionListValidator implements ConstraintValidator<ValidAccessionList, String> {

        @Value("${accessions.max.length}")
        private String maxLength;

        @Override
        public void initialize(ValidAccessionList constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                // verify if accession is valid.
                String[] accessions = value.split("\\s*,\\s*");
                for (String accession : accessions) {
                    if (!accession.matches(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX)) {
                        buildInvalidAccessionMessage(accession, contextImpl);
                        isValid = false;
                    }
                }
                if (accessions.length > getMaxLength()) {
                    buildInvalidAccessionLengthMessage(contextImpl);
                    isValid = false;
                }
                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }
            return isValid;
        }

        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{accessions.invalid.accessions.value}";
            contextImpl.addMessageParameter("0", accession);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        void buildInvalidAccessionLengthMessage(ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{accessions.invalid.accessions.size}";
            contextImpl.addMessageParameter("0", getMaxLength());
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        int getMaxLength() {
            return Integer.parseInt(maxLength);
        }
    }
}
