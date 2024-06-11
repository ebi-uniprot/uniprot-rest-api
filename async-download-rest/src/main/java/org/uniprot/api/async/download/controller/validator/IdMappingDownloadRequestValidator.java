package org.uniprot.api.async.download.controller.validator;

import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;

public interface IdMappingDownloadRequestValidator {

    void validate(IdMappingDownloadRequest request);
}
