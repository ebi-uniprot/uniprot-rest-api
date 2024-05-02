package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 17/06/19
 *
 * @author Edd
 */
@Constraint(
        validatedBy = ValidTSVAndXLSFormatOnlyFields.ValidTSVAndXLSFormatOnlyFieldsValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTSVAndXLSFormatOnlyFields {
    String fieldPattern();

    String message() default "{search.field.invalid.content.type}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class ValidTSVAndXLSFormatOnlyFieldsValidator
            implements ConstraintValidator<ValidTSVAndXLSFormatOnlyFields, String> {

        @Autowired private HttpServletRequest request;

        private String fieldPattern;
        private String message;

        @Override
        public void initialize(ValidTSVAndXLSFormatOnlyFields constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
            try {
                fieldPattern = constraintAnnotation.fieldPattern();
                message = constraintAnnotation.message();
            } catch (Exception e) {
                log.error("Unable to instantiate Valid TSV Format Only Fields", e);
                fieldPattern = "";
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                List<String> fieldsWithPattern = new ArrayList<>();
                String[] fields = value.replaceAll("\\s", "").split(",");
                for (String field : fields) {
                    if (field.matches(getFieldPattern())) {
                        fieldsWithPattern.add(field);
                    }
                }
                String accept = getRequest().getHeader("Accept");

                if (!fieldsWithPattern.isEmpty() && notTSVAndNotXLSFormat(accept)) {
                    isValid = false;
                    ConstraintValidatorContextImpl contextImpl =
                            (ConstraintValidatorContextImpl) context;
                    buildUnsupportedContentTypeErrorMessage(
                            String.join(", ", fieldsWithPattern), contextImpl);
                }
            }

            return isValid;
        }

        private boolean notTSVAndNotXLSFormat(String accept) {
            return accept == null
                    || (!accept.equals(UniProtMediaType.TSV_MEDIA_TYPE_VALUE)
                            && !accept.equals(UniProtMediaType.XLS_MEDIA_TYPE_VALUE));
        }

        HttpServletRequest getRequest() {
            return request;
        }

        void buildUnsupportedContentTypeErrorMessage(
                String invalidFields, ConstraintValidatorContextImpl contextImpl) {
            contextImpl.addMessageParameter("0", invalidFields);
            contextImpl.buildConstraintViolationWithTemplate(message).addConstraintViolation();
            contextImpl.disableDefaultConstraintViolation();
        }

        String getFieldPattern() {
            return fieldPattern;
        }
    }
}
