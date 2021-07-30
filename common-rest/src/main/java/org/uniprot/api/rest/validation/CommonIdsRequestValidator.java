package org.uniprot.api.rest.validation;

import javax.validation.ConstraintValidatorContext;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

/**
 * @author sahmad
 * @created 27/07/2021
 */
public class CommonIdsRequestValidator {
    /**
     * FIXME  fields.split("\\s*,\\s*") pattern
     * https://www.ebi.ac.uk/panda/jira/browse/TRM-26413
     * See other classes for same issue
     * {@link ValidEnumDisplayValue} {@link org.uniprot.api.rest.request.SearchRequest}
     * {@link ValidFacets}
     */
    @SuppressWarnings("squid:S5852")
    boolean isValidReturnFields(
            String fields,
            ReturnFieldConfig returnFieldConfig,
            ConstraintValidatorContext context) {
        boolean isValid = true;
        if (Utils.notNullNotEmpty(fields)) {
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            String[] fieldList = fields.split("\\s*,\\s*");
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

    void buildErrorMessage(String field, ConstraintValidatorContextImpl contextImpl) {
        String errorMessage = "{search.invalid.return.field}";
        contextImpl.addMessageParameter("0", field);
        contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }

    boolean hasValidReturnField(String fieldName, ReturnFieldConfig returnFieldConfig) {
        return returnFieldConfig.returnFieldExists(fieldName);
    }
}
