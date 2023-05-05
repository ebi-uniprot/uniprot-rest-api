package org.uniprot.api.idmapping.controller.validator;

import org.uniprot.api.common.exception.InvalidRequestException;

public class IdMappingDownloadRequestValidatorFactory {

    public IdMappingDownloadRequestValidator create(String type) {
        IdMappingDownloadRequestValidator validator;
        switch (type.toLowerCase()) {
            case "uniprotkb":
                validator = new UniProtKBIdMappingDownloadRequestValidator();
                break;
            case "uniparc":
                validator = new UniParcIdMappingDownloadRequestValidator();
                break;
            case "uniref50":
            case "uniref90":
            case "uniref100":
                validator = new UniRefIdMappingDownloadRequestValidator();
                break;
            default:
                throw new InvalidRequestException(
                        "The IdMapping 'to' parameter value is invalid. It should be 'uniprotkb', 'uniparc', 'uniref50', 'uniref90' or 'uniref100'.");
        }
        return validator;
    }
}
