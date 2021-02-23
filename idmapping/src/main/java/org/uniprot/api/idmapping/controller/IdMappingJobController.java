package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.IdMappingJobController.IDMAPPING_PATH;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.idmapping.controller.request.IdMappingRequest;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.service.HashGenerator;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import javax.validation.Valid;

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
    public JobSubmitResponse submitJob(@Valid IdMappingRequest request) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String jobId = UUID.randomUUID().toString();//TODO call the service layer
        JobSubmitResponse response = new JobSubmitResponse(new HashGenerator().generateHash(request));
        return response;
    }

    // TODO add response class
    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public Object getStatus(String jobId) {
        return null;
    }
}
