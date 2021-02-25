package org.uniprot.api.idmapping.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobStatusResponse;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobService;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.IdMappingJobController.IDMAPPING_PATH;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@RestController
@RequestMapping(IDMAPPING_PATH)
public class IdMappingJobController {
    public static final String IDMAPPING_PATH = "/idmapping";
    private final IdMappingJobService idMappingJobService;

    @Autowired
    public IdMappingJobController(IdMappingJobService idMappingJobService) {
        this.idMappingJobService = idMappingJobService;
    }

    @PostMapping(
            value = "/run",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobSubmitResponse> submitJob(@Valid IdMappingBasicRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        JobSubmitResponse response = this.idMappingJobService.submitJob(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobStatusResponse> getStatus(@PathVariable String jobId) {
        return createStatus(idMappingJobService.getCompletedJob(jobId));
    }

    public ResponseEntity<JobStatusResponse> createStatus(IdMappingJob job) {
        ResponseEntity<JobStatusResponse> response;
        switch (job.getJobStatus()) {
            case NEW:
            case RUNNING:
                response = ResponseEntity.ok(new JobStatusResponse(job.getJobStatus()));
                break;
            case FINISHED:
                String redirectUrl = idMappingJobService.getRedirectPathToResults(job);

                response =
                        ResponseEntity.status(HttpStatus.SEE_OTHER)
                                .header(HttpHeaders.LOCATION, redirectUrl)
                                .body(new JobStatusResponse(JobStatus.FINISHED));
                break;
            default:
                response =
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new JobStatusResponse(JobStatus.ERROR));
                break;
        }

        return response;
    }
}
