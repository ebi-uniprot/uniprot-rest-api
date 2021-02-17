package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.model.IDMappingStringPair;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public interface IDMappingService {
    QueryResult<IDMappingStringPair> fetchIDMappings(IDMappingRequest request);
}
