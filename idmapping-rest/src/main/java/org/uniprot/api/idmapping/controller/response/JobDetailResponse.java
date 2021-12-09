package org.uniprot.api.idmapping.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.uniprot.api.common.repository.search.WarningPair;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lgonzales
 * @since 23/04/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobDetailResponse extends IdMappingJobRequest {
    private String redirectURL;
    private List<WarningPair> warnings;
    private List<WarningPair> errors;
}
