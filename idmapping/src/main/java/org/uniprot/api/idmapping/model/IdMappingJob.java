package org.uniprot.api.idmapping.model;

import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.uniprot.api.idmapping.controller.request.IdMappingRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Data
@Builder
public class IdMappingJob {
    private JobStatus jobStatus;
    private IdMappingRequest idMappingRequest;
    private IdMappingResult idMappingResult;
    private List<String> errorMessages;
    private Date created;
    private Date updated;
}
