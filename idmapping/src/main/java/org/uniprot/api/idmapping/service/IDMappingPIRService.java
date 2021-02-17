package org.uniprot.api.idmapping.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public interface IDMappingPIRService {
    ResponseEntity<String> doPIRRequest(IDMappingRequest request);
}
