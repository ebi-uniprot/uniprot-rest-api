package org.uniprot.api.idmapping.model;

import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Data
@Builder(toBuilder = true)
public class IdMappingJob {
    private String jobId;
    private JobStatus jobStatus;
    private IdMappingBasicRequest idMappingRequest;
    private IdMappingResult idMappingResult;
    private List<String> errorMessages;
//    @Builder.Default private long expiryInMillis = 5 * 60 * 1000; // 5 minutes
    @Builder.Default private Date created = new Date();
    @Builder.Default private Date updated = new Date();
}
