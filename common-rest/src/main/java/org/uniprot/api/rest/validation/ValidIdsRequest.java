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
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

/**
 * @author sahmad
 * @created 26/07/2021
 */
@Constraint(validatedBy = ValidIdsRequest.AccessionListValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIdsRequest {
    String message() default "invalid ids request";

    String accessions() default "accessions";

    String download() default "download";

    String facets() default "facets";

    String facetFilter() default "facetFilter";

    UniProtDataType uniProtDataType();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class AccessionListValidator implements ConstraintValidator<ValidIdsRequest, Object> {

        @Value("${ids.max.download.length}")
        private String maxDownloadLength;

        @Value("${ids.max.search.length}")
        private String maxSearchLength;

        private UniProtDataType dataType;
        private String accessions;
        private String download;
        private String facets;
        private String facetFilter;
        private static final Pattern DOWNLOAD_PATTERN =
                Pattern.compile("^(?:true|false)$", Pattern.CASE_INSENSITIVE);

        @Override
        public void initialize(ValidIdsRequest constraintAnnotation) {
            this.dataType = constraintAnnotation.uniProtDataType();
            this.download = constraintAnnotation.download();
            this.accessions = constraintAnnotation.accessions();
            this.facets = constraintAnnotation.facets();
            this.facetFilter = constraintAnnotation.facetFilter();
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isDownloadValid;
            boolean isDownloadParamsValid = true;
            boolean isCommonParamsValid;
            try {
                String idList = BeanUtils.getProperty(value, this.accessions);
                String downloadValue = BeanUtils.getProperty(value, this.download);
                String facetsValue = BeanUtils.getProperty(value, this.facets);
                String facetFilterValue = BeanUtils.getProperty(value, this.facetFilter);

                isDownloadValid = validateDownloadValue(downloadValue, context);

                if ("true".equalsIgnoreCase(
                                downloadValue)) { // when download=true, facets and facetFilter Not
                                                  // allowed
                    isDownloadParamsValid = validateDownloadSupportedParams(
                                            idList, facetsValue, facetFilterValue, context);
                    isCommonParamsValid = validateAndPopulateErrorMessage(downloadValue,
                                            idList, getMaxDownloadLength(), getDataType(), context);
                } else { // validate for search
                    isCommonParamsValid = validateAndPopulateErrorMessage(downloadValue,
                                    idList, getMaxSearchLength(), getDataType(), context);
                }
            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isDownloadValid = isDownloadParamsValid = isCommonParamsValid = false;
            }

            boolean isValid = isDownloadValid && isDownloadParamsValid && isCommonParamsValid;

            if (!isValid && context != null) {
                context.disableDefaultConstraintViolation();
            }

            return isValid;
        }

        private boolean validateDownloadValue(
                String downloadValue, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(downloadValue)
                    && !DOWNLOAD_PATTERN.matcher(downloadValue).find()) {
                isValid = false;
                buildInvalidDownloadValueMessage(context);
            }
            return isValid;
        }

        void buildInvalidDownloadValueMessage(ConstraintValidatorContext context) {
            String errorMessage = "{search.uniprot.invalid.download}";
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        private boolean validateDownloadSupportedParams(
                String idList,
                String facets,
                String facetFilter,
                ConstraintValidatorContext context) {
            boolean isValid = true;

            if (Utils.notNullNotEmpty(facets)) {
                isValid = false;
                buildFacetsMessage(context);
            }

            if (Utils.notNullNotEmpty(facetFilter)) {
                isValid = false;
                buildFaceFilterMessage(context);
            }
            return isValid;
        }

        private boolean validateAndPopulateErrorMessage(String downloadValue,
                String value,
                int length,
                UniProtDataType dataType,
                ConstraintValidatorContext context) {
            boolean isValid = true;

            if(Utils.nullOrEmpty(value)){
                isValid = false;
                buildEmptyAccessionMessage(context);
            }

            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                // verify if id is valid.
                String[] ids = value.split(",");
                for (String id : ids) {
                    if (!isIdValid(id, dataType)) {
                        buildInvalidAccessionMessage(id, contextImpl);
                        isValid = false;
                    }
                }
                if (ids.length > length) {
                    buildInvalidAccessionLengthMessage(contextImpl, length);
                    isValid = false;
                }
            }
            return isValid;
        }

        private boolean isIdValid(String id, UniProtDataType dataType) {
            switch (dataType) {
                case UNIPROTKB:
                    return id.strip()
                            .toUpperCase()
                            .matches(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);
                case UNIPARC:
                    return id.strip().toUpperCase().matches(FieldRegexConstants.UNIPARC_UPI_REGEX);
                case UNIREF:
                    return id.strip().matches(FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX);
                default:
                    throw new IllegalArgumentException("Unknown UniProtDataType " + dataType);
            }
        }

        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{ids.invalid.ids.value}";
            contextImpl.addMessageParameter("0", accession.strip());
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        void buildInvalidAccessionLengthMessage(
                ConstraintValidatorContextImpl contextImpl, int length) {
            String errorMessage = "{ids.invalid.ids.size}";
            contextImpl.addMessageParameter("0", length);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        void buildFaceFilterMessage(ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate(
                            "Invalid parameter 'facetFilter' for download.")
                    .addConstraintViolation();
        }

        void buildFacetsMessage(ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate("Invalid parameter 'facets' for download.")
                    .addConstraintViolation();
        }

        void buildEmptyAccessionMessage(ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate(
                            "'" + this.accessions + "' is a required parameter")
                    .addConstraintViolation();
        }

        int getMaxSearchLength() {
            return Integer.parseInt(maxSearchLength);
        }

        int getMaxDownloadLength() {
            return Integer.parseInt(maxDownloadLength);
        }

        UniProtDataType getDataType() {
            return this.dataType;
        }
    }
}
