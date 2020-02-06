package org.uniprot.api.rest.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

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
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.Utils;

/**
 * Created 17/06/19
 *
 * @author Edd
 */
@Constraint(validatedBy = ValidContentTypes.ContentTypesValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContentTypes {
    String[] contentTypes();

    String message() default "{search.invalid.contentType}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ContentTypesValidator implements ConstraintValidator<ValidContentTypes, String> {
        private static final Logger LOGGER =
                LoggerFactory.getLogger(ValidFacets.ValidIncludeFacetsValidator.class);

        @Autowired private HttpServletRequest request;

        private List<String> contentTypes;
        private String contentTypesAsString;
        private String message;

        @Override
        public void initialize(ValidContentTypes constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
            try {
                contentTypes = asList(constraintAnnotation.contentTypes());
                contentTypesAsString = String.join(", ", contentTypes);
                message = constraintAnnotation.message();
            } catch (Exception e) {
                LOGGER.error("Unable to instantiate content types config", e);
                contentTypes = emptyList();
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                // validate if the accept is for application/json
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                String accept = getRequest().getHeader("Accept");
                if (accept == null || !getContentTypes().contains(accept)) {
                    buildUnsupportedContentTypeErrorMessage(accept, contextImpl);
                    isValid = false;
                }

                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }

            return isValid;
        }

        HttpServletRequest getRequest() {
            return request;
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
