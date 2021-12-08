package org.uniprot.api.idmapping.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.uniprot.api.idmapping.model.IdMappingWarningError;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Getter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobStatusResponse {
    private final JobStatus jobStatus;
    private final List<IdMappingWarningError> warnings;
    private List<IdMappingWarningError> errors;

    public JobStatusResponse(JobStatus jobStatus){
        this(jobStatus, List.of());
    }

    public JobStatusResponse(List<IdMappingWarningError> errors){
        this(JobStatus.ERROR, List.of(), errors);
    }
    public JobStatusResponse(JobStatus jobStatus, List<IdMappingWarningError> warnings){
        this(jobStatus, warnings, List.of());
    }
    public JobStatusResponse(JobStatus jobStatus, List<IdMappingWarningError> warnings, List<IdMappingWarningError> errors){
        this.jobStatus = jobStatus;
        this.warnings = warnings;
        this.errors = errors;
    }
}
