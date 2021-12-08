package org.uniprot.api.idmapping.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingWarning;

import java.util.List;

/**
 * @author lgonzales
 * @since 23/04/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobDetailResponse extends IdMappingJobRequest {
    private String redirectURL;
    private List<IdMappingWarning> warnings;
}
