package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

/**
 * Created 17/06/19
 *
 * @author Edd
 */
@Constraint(validatedBy = ValidAsyncDownloadFormats.FormatsValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAsyncDownloadFormats {
    String[] formats();

    String message() default "{search.invalid.format}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class FormatsValidator implements ConstraintValidator<ValidAsyncDownloadFormats, String> {
        private static final Logger LOGGER = LoggerFactory.getLogger(FormatsValidator.class);

        private List<String> formats;
        private String formatsAsString;
        private String message;

        @Override
        public void initialize(ValidAsyncDownloadFormats constraintAnnotation) {
            try {
                formats = List.of(constraintAnnotation.formats());
                formatsAsString = String.join(", ", formats);
                message = constraintAnnotation.message();
            } catch (Exception e) {
                LOGGER.error("Exception while initialising ValidAsyncDownloadFormats", e);
                formats = List.of();
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                String type;
                try {
                    type = UniProtMediaType.getMediaTypeForFileExtension(value).toString();
                } catch (IllegalArgumentException ile) {
                    type = "";
                }
                if (!getFormats().contains(value) && !getFormats().contains(type)) {
                    buildUnsupportedFormatErrorMessage(value, contextImpl);
                    isValid = false;
                }

                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }

            return isValid;
        }

        Collection<String> getFormats() {
            return formats;
        }

        void buildUnsupportedFormatErrorMessage(
                String format, ConstraintValidatorContextImpl contextImpl) {
            contextImpl.addMessageParameter("0", format);
            contextImpl.addMessageParameter("1", formatsAsString);
            contextImpl.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }
    }
}
