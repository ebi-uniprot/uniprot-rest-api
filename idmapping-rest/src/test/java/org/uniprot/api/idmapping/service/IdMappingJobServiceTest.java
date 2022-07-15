package org.uniprot.api.idmapping.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.impl.IdMappingJobServiceImpl;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {IdMappingJobServiceImpl.class, TestConfig.class, DataStoreTestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingJobServiceTest {
    @Autowired private IdMappingJobServiceImpl jobService;
    @MockBean private IdMappingPIRService pirService;
    @Autowired private IdMappingJobCacheService cacheService;
    @MockBean private ServletContext servletContext;

    @Test
    void testSubmitJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        IdMappingJobRequest request = createIdMappingRequest();
        JobSubmitResponse response = this.jobService.submitJob(request);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getJobId());
    }

    @Disabled
    @Test
    void testFinishedJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        // when
        IdMappingJobRequest request = createIdMappingRequest();
        when(this.pirService.mapIds(request, "dummyJobId"))
                .thenReturn(
                        IdMappingResult.builder()
                                .mappedId(new IdMappingStringPair("from", "to"))
                                .build());

        JobSubmitResponse submitResponse = this.jobService.submitJob(request);
        Assertions.assertNotNull(submitResponse);
        Assertions.assertNotNull(submitResponse.getJobId());
        // then
        Thread.sleep(5000); // delay to make sure that job is running
        String jobId = submitResponse.getJobId();
        IdMappingJob submittedJob = this.cacheService.getJobAsResource(jobId);
        Assertions.assertNotNull(submittedJob);
        Assertions.assertEquals(jobId, submittedJob.getJobId());
        Assertions.assertNotNull(submittedJob.getJobStatus());
        Assertions.assertNull(submittedJob.getErrors());
        Assertions.assertEquals(request, submittedJob.getIdMappingRequest());
        Assertions.assertNotNull(submittedJob.getIdMappingResult());
        Assertions.assertNotNull(submittedJob.getCreated());
        Assertions.assertNotNull(submittedJob.getUpdated());
    }

    @Test
    void testRunningJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        // when
        IdMappingJobRequest request = createIdMappingRequest();
        Mockito.doAnswer(
                        new AnswersWithDelay(
                                1500,
                                new Returns(
                                        IdMappingResult.builder()
                                                .mappedId(new IdMappingStringPair("from", "to"))
                                                .build())))
                .when(this.pirService)
                .mapIds(request, "dummyJobId");

        JobSubmitResponse submitResponse = this.jobService.submitJob(request);
        Assertions.assertNotNull(submitResponse);
        Assertions.assertNotNull(submitResponse.getJobId());
        // then
        Thread.sleep(500); // to make sure that task is picked to run
        String jobId = submitResponse.getJobId();
        IdMappingJob submittedJob = this.cacheService.getJobAsResource(jobId);
        Assertions.assertNotNull(submittedJob);
        Assertions.assertEquals(jobId, submittedJob.getJobId());
        Assertions.assertEquals(JobStatus.RUNNING, submittedJob.getJobStatus());
        Assertions.assertNull(submittedJob.getErrors());
        Assertions.assertEquals(request, submittedJob.getIdMappingRequest());
        Assertions.assertNull(submittedJob.getIdMappingResult());
        Assertions.assertNotNull(submittedJob.getCreated());
        Assertions.assertNotNull(submittedJob.getUpdated());
    }

    @Disabled
    @Test
    void testErroredJob()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        // when
        IdMappingJobRequest request = createIdMappingRequest();
        String errorMsg = "Error during rest call";
        when(this.pirService.mapIds(request, "dummyJobId"))
                .thenThrow(new RestClientException(errorMsg))
                .thenReturn(IdMappingResult.builder().build());

        JobSubmitResponse submitResponse = this.jobService.submitJob(request);
        Assertions.assertNotNull(submitResponse);
        Assertions.assertNotNull(submitResponse.getJobId());
        // then
        String jobId = submitResponse.getJobId();
        IdMappingJob submittedJob = null;
        int attemptsRemaining = 5;
        while (attemptsRemaining-- > 0) {
            Thread.sleep(3000); // delay to make sure that thread is picked to run
            submittedJob = this.cacheService.getJobAsResource(jobId);
            if (submittedJob != null) {
                break;
            }
        }
        Assertions.assertNotNull(submittedJob);
        Assertions.assertEquals(jobId, submittedJob.getJobId());
        Assertions.assertEquals(JobStatus.ERROR, submittedJob.getJobStatus());
        Assertions.assertNotNull(submittedJob.getErrors());
        Assertions.assertEquals(1, submittedJob.getErrors().size());
        Assertions.assertEquals(errorMsg, submittedJob.getErrors().get(0).getMessage());
        Assertions.assertEquals(50, submittedJob.getErrors().get(0).getCode());
        Assertions.assertEquals(request, submittedJob.getIdMappingRequest());
        Assertions.assertNull(submittedJob.getIdMappingResult());
        Assertions.assertNotNull(submittedJob.getCreated());
        Assertions.assertNotNull(submittedJob.getUpdated());
        Mockito.verify(pirService, times(1)).mapIds(request, "dummyJobId");

        this.jobService.submitJob(request);
        IdMappingJob newJobAsResource = this.cacheService.getJobAsResource(jobId);
        JobStatus currentStatus = newJobAsResource.getJobStatus();
        MatcherAssert.assertThat(currentStatus, IsIn.oneOf(JobStatus.NEW, JobStatus.RUNNING));
    }

    @Nested
    class RedirectTests {
        private IdMappingJobService idMappingJobService;

        @BeforeEach
        void setUp() {
            ServletContext mockContext = mock(ServletContext.class);
            when(mockContext.getContextPath()).thenReturn("/proteins/api");
            idMappingJobService = new IdMappingJobServiceImpl(null, null, null, null);
        }

        @ParameterizedTest(name = "{index}: {0}")
        @ValueSource(
                strings = {
                    "jobId + UniRef50 ->  https://localhost/proteins/api/idmapping/uniref/results/jobId",
                    "jobId + UniRef90 ->  https://localhost/proteins/api/idmapping/uniref/results/jobId",
                    "jobId + UniRef100 -> https://localhost/proteins/api/idmapping/uniref/results/jobId",
                    "jobId + UniParc ->   https://localhost/proteins/api/idmapping/uniparc/results/jobId",
                    "jobId + UniProtKB -> https://localhost/proteins/api/idmapping/uniprotkb/results/jobId",
                    "jobId + ANYTHING ->  https://localhost/proteins/api/idmapping/results/jobId"
                })
        void checkValidRedirectionLocations(String source) {
            String requestUrl =
                    "http://localhost/proteins/api/idmapping/run?a=parameter&another=parameter";
            String[] sourceParts = source.split(" \\+ ");
            String jobId = sourceParts[0];
            String remainder = sourceParts[1];

            sourceParts = remainder.split("[ ]+->[ ]+");
            String toDb = sourceParts[0];
            String urlPart = sourceParts[1];

            IdMappingJobRequest jobRequest = new IdMappingJobRequest();
            jobRequest.setTo(toDb);
            IdMappingJob job =
                    IdMappingJob.builder().jobId(jobId).idMappingRequest(jobRequest).build();
            String redirectPathToResults =
                    idMappingJobService.getRedirectPathToResults(job, requestUrl);
            assertThat(redirectPathToResults, is(urlPart));
        }
    }

    @Test
    void testGetUnknownJob() {
        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> this.cacheService.getJobAsResource("some random id"));
    }

    @Test
    void testSubmitSameJobTwice()
            throws InvalidKeySpecException, NoSuchAlgorithmException, InterruptedException {
        IdMappingJobRequest request = createIdMappingRequest();
        JobSubmitResponse response = this.jobService.submitJob(request);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getJobId());
        IdMappingJob job = this.cacheService.get(response.getJobId());
        Date created = job.getCreated();
        Assertions.assertNotNull(created);
        Date updated = job.getUpdated();
        Assertions.assertNotNull(updated);
        // submit the same job
        Thread.sleep(1);
        JobSubmitResponse response2 = this.jobService.submitJob(request);
        Assertions.assertNotNull(response2);
        Assertions.assertEquals(response.getJobId(), response2.getJobId());
        Assertions.assertEquals(job.getCreated(), created);
        Assertions.assertNotEquals(updated, job.getUpdated());
        Assertions.assertTrue((job.getUpdated().getTime() - updated.getTime()) > 0);
    }

    private IdMappingJobRequest createIdMappingRequest() {
        String random = UUID.randomUUID().toString();
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom("from" + random);
        request.setTo("to" + random);
        request.setIds("ids" + random);
        return request;
    }
}
