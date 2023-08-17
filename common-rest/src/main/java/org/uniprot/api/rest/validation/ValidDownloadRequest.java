package org.uniprot.api.rest.validation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.http.MediaType;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.core.util.Utils;

@Constraint(validatedBy = ValidDownloadRequest.DownloadRequestValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDownloadRequest {

    /** formats where 'fields' param doesn't make sense and hence not allowed */
    List<String> FORMATS_WITH_NO_PROJECTION =
            List.of(
                    FF_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE,
                    MediaType.APPLICATION_XML_VALUE,
                    FASTA_MEDIA_TYPE_VALUE,
                    GFF_MEDIA_TYPE_VALUE,
                    RDF_MEDIA_TYPE_VALUE,
                    TURTLE_MEDIA_TYPE_VALUE,
                    N_TRIPLES_MEDIA_TYPE_VALUE,
                    HDF5_MEDIA_TYPE_VALUE);

    String message() default "{async.download.invalid.param}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class DownloadRequestValidator
            implements ConstraintValidator<ValidDownloadRequest, DownloadRequest> {

        private String message;

        @Override
        public void initialize(ValidDownloadRequest constraintAnnotation) {
            this.message = constraintAnnotation.message();
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(
                DownloadRequest downloadRequest, ConstraintValidatorContext context) {
            boolean isValid = true;
            try {
                String fields = BeanUtils.getProperty(downloadRequest, "fields");
                if (Utils.notNullNotEmpty(fields)) {
                    ConstraintValidatorContextImpl contextImpl =
                            (ConstraintValidatorContextImpl) context;

                    String format = getPassedOrDefaultFormat(downloadRequest);

                    String type = getMediaTypeFromShortName(format);

                    if (FORMATS_WITH_NO_PROJECTION.contains(format)
                            || FORMATS_WITH_NO_PROJECTION.contains(type)) {
                        buildInvalidParamFieldsErrorMessage(format, contextImpl);
                        isValid = false;
                    }

                    if (!isValid && context != null) {
                        context.disableDefaultConstraintViolation();
                    }
                }

            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isValid = false;
            }
            return isValid;
        }

        private static String getMediaTypeFromShortName(String format) {
            String type;
            try {
                type = UniProtMediaType.getMediaTypeForFileExtension(format).toString();
            } catch (IllegalArgumentException ile) {
                type = "";
            }
            return type;
        }

        private String getPassedOrDefaultFormat(DownloadRequest downloadRequest)
                throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            String format = BeanUtils.getProperty(downloadRequest, "format");
            if (Utils.nullOrEmpty(format)) {
                format = APPLICATION_JSON_VALUE;
            }
            return format;
        }

        void buildInvalidParamFieldsErrorMessage(
                String format, ConstraintValidatorContextImpl contextImpl) {
            contextImpl.addMessageParameter("format", format);
            contextImpl.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }
    }
}
