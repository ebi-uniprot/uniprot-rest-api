package org.uniprot.api.idmapping.controller.validator;

import org.uniprot.api.idmapping.common.request.IdMappingDownloadRequest;

public interface IdMappingDownloadRequestValidator {

    void validate(IdMappingDownloadRequest request);
}
