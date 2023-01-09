package org.uniprot.api.idmapping.controller.response;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.rest.download.model.JobStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Getter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobStatusResponse {
    private final JobStatus jobStatus;
    private final List<ProblemPair> warnings;
    private List<ProblemPair> errors;

    public JobStatusResponse(JobStatus jobStatus) {
        this(jobStatus, List.of());
    }

    public JobStatusResponse(List<ProblemPair> errors) {
        this(JobStatus.ERROR, List.of(), errors);
    }

    public JobStatusResponse(JobStatus jobStatus, List<ProblemPair> warnings) {
        this(jobStatus, warnings, List.of());
    }

    public JobStatusResponse(
            JobStatus jobStatus, List<ProblemPair> warnings, List<ProblemPair> errors) {
        this.jobStatus = jobStatus;
        this.warnings = warnings;
        this.errors = errors;
    }
}
