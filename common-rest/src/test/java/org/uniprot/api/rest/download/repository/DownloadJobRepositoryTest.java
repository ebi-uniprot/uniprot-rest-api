package org.uniprot.api.rest.download.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

import java.util.Optional;


/**
 * @author sahmad
 * @created 22/12/2022
 */
@ExtendWith(SpringExtension.class)
public class DownloadJobRepositoryTest {

    @MockBean
    private DownloadJobRepository jobRepository;
    @Test
    public void whenSavingJob_thenAvailableOnRetrieval() throws Exception {
        String jobId = "123456789";
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
        jobBuilder.id(jobId).query("test query").fields("field1,field2,field3");
        jobBuilder.retried(1).status(JobStatus.NEW);
        DownloadJob job = jobBuilder.build();
        when(jobRepository.save(job)).thenReturn(job);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        DownloadJob savedJob = jobRepository.save(job);
        Assertions.assertNotNull(savedJob);
        Assertions.assertEquals(savedJob, job);
        DownloadJob retrievedJob = jobRepository.findById(job.getId()).get();
        Assertions.assertEquals(job.getId(), retrievedJob.getId());
    }
}
