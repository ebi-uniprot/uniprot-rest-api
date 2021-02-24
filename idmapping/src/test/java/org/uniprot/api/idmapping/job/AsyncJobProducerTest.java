package org.uniprot.api.idmapping.job;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.HashGenerator;
import org.uniprot.api.idmapping.service.job.AsyncJobProducer;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author sahmad
 * @created 24/02/2021
 */
//@ExtendWith(SpringExtension.class)
//@ContextConfiguration(classes = {AsyncJobProducer.class, JobConfig.class})
class AsyncJobProducerTest {
    @Test
    void testEnqueueJob() throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        BlockingQueue<IdMappingJob> queue = new LinkedBlockingQueue<>(1);
        AsyncJobProducer jobProducer = new AsyncJobProducer(queue);
        IdMappingJob job = createIdMappingJob();
        jobProducer.enqueueJob(job);
        // verify the queue
        Assertions.assertEquals(1, queue.size());
        Assertions.assertEquals(job, queue.take());

    }

    private IdMappingJob createIdMappingJob() throws InvalidKeySpecException, NoSuchAlgorithmException {
        IdMappingBasicRequest request = new IdMappingBasicRequest();
        request.setFrom("from");
        request.setTo("to");
        request.setIds("ids");
        HashGenerator hashGenerator = new HashGenerator();
        String jobId = hashGenerator.generateHash(request);
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(JobStatus.NEW);
        builder.idMappingRequest(request);
        return builder.build();
    }
}
