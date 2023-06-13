package org.uniprot.api.idmapping.controller.validator;

import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;

public interface IdMappingDownloadRequestValidator {

    void validate(IdMappingDownloadRequest request);
}
