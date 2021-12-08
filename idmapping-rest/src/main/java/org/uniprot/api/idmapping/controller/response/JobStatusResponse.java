package org.uniprot.api.idmapping.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.uniprot.api.idmapping.model.IdMappingWarning;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
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
    private final List<IdMappingWarning> warnings;

    public JobStatusResponse(JobStatus jobStatus){
        this(jobStatus, new ArrayList<>());
    }

    public JobStatusResponse(JobStatus jobStatus, List<IdMappingWarning> warnings){
        this.jobStatus = jobStatus;
        this.warnings = warnings;
    }
}
