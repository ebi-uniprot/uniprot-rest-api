package org.uniprot.api.idmapping.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdMappingJobService.class, TestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingJobServiceTest {
    @Autowired
    private IdMappingJobService jobService;

    @Test
    void testSubmitJob() throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        IdMappingBasicRequest request = createIdMappingRequest();
        JobSubmitResponse response = this.jobService.submitJob(request);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getJobId());
    }

    private IdMappingBasicRequest createIdMappingRequest() {
        String random = UUID.randomUUID().toString();
        IdMappingBasicRequest request = new IdMappingBasicRequest();
        request.setFrom("from" + random);
        request.setTo("to" + random);
        request.setIds("ids" + random);
        return request;
    }

}
