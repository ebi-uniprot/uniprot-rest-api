package org.uniprot.api.idmapping.common.request;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.common.repository.search.ProblemPair;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author lgonzales
 * @since 23/04/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobDetailResponse extends IdMappingJobRequest {
    private String redirectURL;
    private List<ProblemPair> warnings;
    private List<ProblemPair> errors;
}
