package org.uniprot.api.idmapping.service;

import org.springframework.http.ResponseEntity;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public interface IDMappingPIRService {
    ResponseEntity<String> doPIRRequest(IdMappingBasicRequest request);
}
