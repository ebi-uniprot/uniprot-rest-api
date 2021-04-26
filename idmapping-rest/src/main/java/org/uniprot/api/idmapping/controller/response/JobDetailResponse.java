package org.uniprot.api.idmapping.controller.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;

/**
 * @author lgonzales
 * @since 23/04/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobDetailResponse extends IdMappingJobRequest {

    private String redirectURL;
}
