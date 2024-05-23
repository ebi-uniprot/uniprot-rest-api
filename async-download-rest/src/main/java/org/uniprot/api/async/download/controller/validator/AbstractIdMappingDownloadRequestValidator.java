package org.uniprot.api.async.download.controller.validator;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

public abstract class AbstractIdMappingDownloadRequestValidator
        implements IdMappingDownloadRequestValidator {

    @Override
    public void validate(IdMappingDownloadRequest request) {
        validateFormat(request.getFormat());
        validateReturnFields(request.getFields());
    }

    void validateFormat(String format) {
        List<String> validFormats = getValidFormats();
        if (format == null || !validFormats.contains(format)) {
            throw new InvalidRequestException(
                    "Invalid download format. Valid values are " + validFormats);
        }
    }

    void validateReturnFields(String input) {
        if (input != null) { // fields is optional
            ReturnFieldConfig validFields = getReturnFieldConfig();
            List<String> invalidField = new ArrayList<>();
            String[] fieldArray = input.split(",");
            for (String field : fieldArray) {
                if (!validFields.returnFieldExists(field.strip())) {
                    invalidField.add(field.strip());
                }
            }

            if (!invalidField.isEmpty()) {
                String errorMessage = "Invalid " + getType() + " fields parameter value";
                if (invalidField.size() > 1) {
                    errorMessage += "s";
                }
                errorMessage += ": " + String.join(", ", invalidField + ".");
                throw new InvalidRequestException(errorMessage);
            }
        }
    }

    protected abstract String getType();

    protected abstract ReturnFieldConfig getReturnFieldConfig();

    protected abstract List<String> getValidFormats();
}
