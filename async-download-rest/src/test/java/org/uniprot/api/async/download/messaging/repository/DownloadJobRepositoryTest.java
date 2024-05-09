package org.uniprot.api.async.download.messaging.repository;

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@ExtendWith(SpringExtension.class)
class DownloadJobRepositoryTest {

    @MockBean private UniProtKBDownloadJobRepository jobRepository;

    @Test
    void whenSavingJob_thenAvailableOnRetrieval() throws Exception {
        String jobId = "123456789";
        UniProtKBDownloadJob.UniProtKBDownloadJobBuilder jobBuilder =
                UniProtKBDownloadJob.builder();
        jobBuilder.id(jobId).query("test query").fields("field1,field2,field3");
        jobBuilder.retried(1).status(JobStatus.NEW);
        UniProtKBDownloadJob job = jobBuilder.build();
        when(jobRepository.save(job)).thenReturn(job);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        DownloadJob savedJob = jobRepository.save(job);
        Assertions.assertNotNull(savedJob);
        Assertions.assertEquals(savedJob, job);
        DownloadJob retrievedJob = jobRepository.findById(job.getId()).get();
        Assertions.assertEquals(job.getId(), retrievedJob.getId());
    }
}
