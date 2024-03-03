package org.uniprot.api.async.download.model.common;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

@Constraint(validatedBy = ValidDownloadRequest.DownloadRequestValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDownloadRequest {

    /** formats where 'fields' param doesn't make sense and hence not allowed */
    Set<String> FORMATS_WITH_NO_PROJECTION =
            Set.of(
                    FF_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE,
                    APPLICATION_XML_VALUE,
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

        private static final String FORMAT_STR = "format";
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
                String fields = getPassedParamValue(downloadRequest, "fields");
                if (Utils.notNullNotEmpty(fields)) {
                    if (isPassedWithInvalidFormat(downloadRequest)) {
                        ConstraintValidatorContextImpl contextImpl =
                                (ConstraintValidatorContextImpl) context;
                        buildInvalidParamFieldsErrorMessage(
                                getPassedParamValue(downloadRequest, FORMAT_STR), contextImpl);
                        isValid = false;
                    }

                    if (!isValid) {
                        context.disableDefaultConstraintViolation();
                    }
                }

            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isValid = false;
            }
            return isValid;
        }

        private boolean isPassedWithInvalidFormat(DownloadRequest downloadRequest)
                throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            String format = getPassedOrDefaultFormat(downloadRequest);
            String type = getMediaTypeFromShortName(format);
            return FORMATS_WITH_NO_PROJECTION.contains(format)
                    || FORMATS_WITH_NO_PROJECTION.contains(type);
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
            String format = getPassedParamValue(downloadRequest, FORMAT_STR);
            if (Utils.nullOrEmpty(format)) {
                format = APPLICATION_JSON_VALUE;
            }
            return format;
        }

        private String getPassedParamValue(DownloadRequest downloadRequest, String paramName)
                throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            return BeanUtils.getProperty(downloadRequest, paramName);
        }

        void buildInvalidParamFieldsErrorMessage(
                String format, ConstraintValidatorContextImpl contextImpl) {
            contextImpl.addMessageParameter(FORMAT_STR, format);
            contextImpl.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }
    }
}
