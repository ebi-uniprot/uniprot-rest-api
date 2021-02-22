package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.controller.IdMappingJobController.IDMAPPING_PATH;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@RestController
@RequestMapping(IDMAPPING_PATH)
public class IdMappingJobController {
    static final String IDMAPPING_PATH = "/idmapping";
    // TODO add request and response classes
    @PostMapping(
            value = "/run",
            produces = {APPLICATION_JSON_VALUE})
    public Object submitJob(Object request) {
        return null;
    }

    // TODO add response class
    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public Object getStatus(String jobId) {
        return null;
    }
}
