package org.uniprot.api.rest.validation;

import static java.util.Arrays.asList;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.uniprot.core.util.Utils;

/**
 * Created 17/06/19
 *
 * @author Edd
 */
@Constraint(validatedBy = ValidAsyncDownloadContentTypes.ContentTypesValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAsyncDownloadContentTypes {
    String[] contentTypes();

    String message() default "{search.invalid.contentType}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ContentTypesValidator
            implements ConstraintValidator<ValidAsyncDownloadContentTypes, String> {
        private static final Logger LOGGER =
                LoggerFactory.getLogger(ValidAsyncDownloadContentTypes.ContentTypesValidator.class);

        @Autowired private HttpServletRequest request;

        private List<String> contentTypes;
        private String contentTypesAsString;
        private String message;

        @Override
        public void initialize(ValidAsyncDownloadContentTypes constraintAnnotation) {
            try {
                contentTypes = asList(constraintAnnotation.contentTypes());
                contentTypesAsString = String.join(", ", contentTypes);
                message = constraintAnnotation.message();
            } catch (Exception e) {
                LOGGER.error("Exception while initialising ValidAsyncDownloadContentTypes", e);
                contentTypes = List.of();
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                if (!getContentTypes().contains(value)) {
                    buildUnsupportedContentTypeErrorMessage(value, contextImpl);
                    isValid = false;
                }

                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }

            return isValid;
        }

        Collection<String> getContentTypes() {
            return contentTypes;
        }

        void buildUnsupportedContentTypeErrorMessage(
                String contentType, ConstraintValidatorContextImpl contextImpl) {
            contextImpl.addMessageParameter("0", contentType);
            contextImpl.addMessageParameter("1", contentTypesAsString);
            contextImpl.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }
    }
}
