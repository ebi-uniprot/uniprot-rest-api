package org.uniprot.api.idmapping.controller;

import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public interface JobOperation {
    IdMappingJob createAndPutJobInCache() throws Exception;
}
