package org.uniprot.api.idmapping.common.request;

import java.util.List;

import org.uniprot.api.common.repository.search.ProblemPair;

import com.fasterxml.jackson.annotation.JsonInclude;

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
    private String query;
    private Boolean includeIsoform;
    private List<ProblemPair> warnings;
    private List<ProblemPair> errors;
}
