package org.uniprot.api.idmapping.service;

import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public interface IDMappingPIRService {
    IdMappingResult doPIRRequest(IdMappingBasicRequest request);
}
