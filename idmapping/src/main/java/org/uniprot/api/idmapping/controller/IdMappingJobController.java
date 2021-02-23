package org.uniprot.api.idmapping.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.idmapping.controller.request.IdMappingRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobStatusResponse;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.IdMappingJobController.IDMAPPING_PATH;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@RestController
@RequestMapping(IDMAPPING_PATH)
public class IdMappingJobController {
    static final String IDMAPPING_PATH = "/idmapping";

    @PostMapping(
            value = "/run",
            produces = {APPLICATION_JSON_VALUE})
    public JobSubmitResponse submitJob(IdMappingRequest request) {
        String jobId = UUID.randomUUID().toString(); // TODO call the service layer
        JobSubmitResponse response = new JobSubmitResponse(jobId);
        return response;
    }

    // TODO add response class
    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobStatusResponse> getStatus(String jobId) {

        JobStatus jobStatus =
                getJobStatus(jobId); // retrieve status from cache, but pretend it's running
        try {
            if (jobStatus == JobStatus.RUNNING) {
                return ResponseEntity.ok(new JobStatusResponse(JobStatus.RUNNING));
            } else if (jobStatus == JobStatus.FINISHED) {
                String redirectUrl = "http://myapi/result/" + jobId;
                return ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header(HttpHeaders.LOCATION, redirectUrl)
                        .body(new JobStatusResponse(JobStatus.FINISHED));
            } else if (jobStatus == JobStatus.NOT_FOUND) {
                return ResponseEntity.ok(new JobStatusResponse(JobStatus.NOT_FOUND));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new JobStatusResponse(JobStatus.ERROR));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new JobStatusResponse(JobStatus.ERROR));
        }
    }

    private JobStatus getJobStatus(String jobId) {
        // this should retrieve data from cache, but is stubbed for now
        if (jobId.startsWith("R")) {
            return JobStatus.RUNNING;
        } else if (jobId.startsWith("F")) {
            return JobStatus.FINISHED;
        } else if (jobId.startsWith("N")) {
            return JobStatus.NOT_FOUND;
        }
        return JobStatus.ERROR;
    }
}
