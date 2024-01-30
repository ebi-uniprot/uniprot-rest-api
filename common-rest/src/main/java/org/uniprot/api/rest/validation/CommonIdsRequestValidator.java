package org.uniprot.api.rest.validation;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPROTKB_ACCESSION_OPTIONAL_SEQ_RANGE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

/**
 * @author sahmad
 * @created 27/07/2021
 */
public abstract class CommonIdsRequestValidator {
    HttpServletRequest getHttpServletRequest() {
        return null;
    }

    boolean isValidReturnFields(
            String fields,
            ReturnFieldConfig returnFieldConfig,
            ConstraintValidatorContext context) {
        boolean isValid = true;
        if (Utils.notNullNotEmpty(fields)) {
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            String[] fieldList = fields.replaceAll("\\s", "").split(",");
            for (String field : fieldList) {
                if (!hasValidReturnField(field, returnFieldConfig)) {
                    buildErrorMessage(field, contextImpl);
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    boolean validateIdsAndPopulateErrorMessage(
            String commaSeparatedIds,
            int length,
            UniProtDataType dataType,
            ConstraintValidatorContext context) {
        boolean isValid = true;
        if (Utils.notNullNotEmpty(commaSeparatedIds)) {
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            // verify if id is valid.
            String[] ids = commaSeparatedIds.split(",");
            String passedFormat = getHttpServletRequest().getHeader("Accept");
            for (String id : ids) {
                if (!isIdValid(id, dataType)) {
                    buildInvalidAccessionMessage(id, contextImpl);
                    isValid = false;
                }
                if (!isValidFormatForSubsequence(id, passedFormat, dataType)) {
                    buildInvalidFormatErrorMessage(passedFormat, contextImpl);
                    isValid = false;
                    break;
                }

                if (!isValidSequenceRange(id, dataType)) {
                    invalidSequeceRangeMessage(id, contextImpl);
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
                return UNIPROTKB_ACCESSION_OPTIONAL_SEQ_RANGE
                        .matcher(id.strip().toUpperCase())
                        .matches();
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

    void buildErrorMessage(String field, ConstraintValidatorContextImpl contextImpl) {
        String errorMessage = "{search.invalid.return.field}";
        contextImpl.addMessageParameter("0", field);
        contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }

    void buildInvalidFormatErrorMessage(
            String passedFormat, ConstraintValidatorContextImpl contextImpl) {
        String errorMessage = "{search.invalid.contentType}";
        contextImpl.addMessageParameter("0", passedFormat);
        contextImpl.addMessageParameter("1", UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);
        contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }

    boolean hasValidReturnField(String fieldName, ReturnFieldConfig returnFieldConfig) {
        return returnFieldConfig.returnFieldExists(fieldName);
    }

    boolean isValidFormatForSubsequence(String id, String passedFormat, UniProtDataType dataType) {
        boolean isValid = true;
        if (UniProtDataType.UNIPROTKB == dataType
                && UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX
                        .matcher(id.strip().toUpperCase())
                        .matches()
                && !UniProtMediaType.FASTA_MEDIA_TYPE_VALUE.equals(passedFormat)) {
            isValid = false;
        }
        return isValid;
    }

    boolean isValidSequenceRange(String id, UniProtDataType dataType) {
        boolean isValid = true;
        if (UniProtDataType.UNIPROTKB == dataType
                && UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX
                        .matcher(id.strip().toUpperCase())
                        .matches()) {
            String range = id.substring(id.indexOf('[') + 1, id.indexOf(']'));
            String[] rangeTokens = range.split("-");

            if (rangeTokens.length != 2) {
                return false;
            }

            try {
                int start = Integer.parseInt(rangeTokens[0]);
                int end = Integer.parseInt(rangeTokens[1]);
                if (start <= 0 || start > end) {
                    isValid = false;
                }
            } catch (NumberFormatException nfe) {
                isValid = false;
            }
        }
        return isValid;
    }

    void invalidSequeceRangeMessage(String id, ConstraintValidatorContextImpl contextImpl) {
        String errMsg = "Invalid sequence range '{0}' in the accession.";
        contextImpl.addMessageParameter("0", id);
        contextImpl.buildConstraintViolationWithTemplate(errMsg).addConstraintViolation();
    }
}
