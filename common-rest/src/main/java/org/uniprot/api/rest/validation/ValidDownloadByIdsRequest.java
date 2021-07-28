package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

/**
 * @author sahmad
 * @created 26/07/2021
 */
@Constraint(validatedBy = ValidDownloadByIdsRequest.AccessionListValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDownloadByIdsRequest {
    String message() default "invalid ids download request";

    String accessions() default "accessions";

    String download() default "download";

    String fields() default "fields";

    UniProtDataType uniProtDataType();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class AccessionListValidator extends CommonIdsRequestValidator
            implements ConstraintValidator<ValidDownloadByIdsRequest, Object> {

        @Value("${ids.max.download.length}")
        private String maxDownloadLength;

        private UniProtDataType uniProtDataType;
        private String accessions;
        private String download;
        private String fields;
        private static final Pattern DOWNLOAD_PATTERN =
                Pattern.compile("^(true)$", Pattern.CASE_INSENSITIVE);
        private ReturnFieldConfig returnFieldConfig;

        @Override
        public void initialize(ValidDownloadByIdsRequest constraintAnnotation) {
            this.uniProtDataType = constraintAnnotation.uniProtDataType();
            this.download = constraintAnnotation.download();
            this.accessions = constraintAnnotation.accessions();
            this.fields = constraintAnnotation.fields();
            this.returnFieldConfig = ReturnFieldConfigFactory.getReturnFieldConfig(uniProtDataType);
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isValid;
            try {
                String idList = BeanUtils.getProperty(value, this.accessions);
                String downloadValue = BeanUtils.getProperty(value, this.download);
                String fieldsValue = BeanUtils.getProperty(value, this.fields);

                boolean isDownloadValid = validateDownloadValue(downloadValue, context);
                boolean isValidReturnFields =
                        isValidReturnFields(fieldsValue, this.returnFieldConfig, context);

                boolean isIdListValid =
                        validateIdsAndPopulateErrorMessage(
                                idList, getMaxDownloadLength(), getDataType(), context);
                isValid = isDownloadValid && isValidReturnFields && isIdListValid;

                if (!isValid && context != null) {
                    context.disableDefaultConstraintViolation();
                }
            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isValid = false;
            }

            return isValid;
        }

        private boolean validateDownloadValue(
                String downloadValue, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.nullOrEmpty(downloadValue)) {
                isValid = false;
                buildRequiredFieldMessage(context, "'download' is a required parameter");
            } else if (!DOWNLOAD_PATTERN.matcher(downloadValue).find()) {
                isValid = false;
                buildInvalidDownloadValueMessage(context);
            }
            return isValid;
        }

        @Override
        boolean validateIdsAndPopulateErrorMessage(
                String commaSeparatedIds,
                int length,
                UniProtDataType dataType,
                ConstraintValidatorContext context) {
            boolean isValid = true;

            if (Utils.nullOrEmpty(commaSeparatedIds)) {
                isValid = false;
                buildRequiredFieldMessage(
                        context, "'" + this.accessions + "' is a required parameter");
            }
            return isValid
                    && super.validateIdsAndPopulateErrorMessage(
                            commaSeparatedIds, length, dataType, context);
        }

        void buildRequiredFieldMessage(ConstraintValidatorContext context, String message) {
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        void buildInvalidDownloadValueMessage(ConstraintValidatorContext context) {
            String errorMessage = "{search.uniprot.invalid.download.only}";
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        void buildEmptyAccessionMessage(ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate("{search.required}")
                    .addConstraintViolation();
        }

        int getMaxDownloadLength() {
            return Integer.parseInt(maxDownloadLength);
        }

        UniProtDataType getDataType() {
            return this.uniProtDataType;
        }
    }
}
