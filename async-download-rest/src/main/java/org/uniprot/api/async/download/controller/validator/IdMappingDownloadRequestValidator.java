package org.uniprot.api.async.download.controller.validator;

import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequest;

public interface IdMappingDownloadRequestValidator {

    void validate(IdMappingDownloadRequest request);
}
