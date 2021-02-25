package org.uniprot.api.idmapping.controller.request;

import org.uniprot.api.rest.request.StreamRequest;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
public interface IdMappingStreamRequest extends StreamRequest {

    String getJobId();
}
