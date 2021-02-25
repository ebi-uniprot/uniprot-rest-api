package org.uniprot.api.idmapping.controller.request;

import org.uniprot.api.rest.request.SearchRequest;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
public interface IdMappingSearchRequest extends SearchRequest {

    String getJobId();
}
