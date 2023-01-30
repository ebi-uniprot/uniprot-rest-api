package org.uniprot.api.rest.download.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@ActiveProfiles(profiles = "offline")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RedisConfiguration.class})
public class DownloadJobRepositoryTest {

    @Autowired private DownloadJobRepository jobRepository;

    // TODO use testcontainer for this test tow ork
    @Test
    public void whenSavingJob_thenAvailableOnRetrieval() throws Exception {
        DownloadJob.DownloadJobBuilder jobBuilder = DownloadJob.builder();
        jobBuilder.id("123456789").query("test query").fields("field1,field2,field3");
        jobBuilder.retried(1).status(JobStatus.NEW);
        DownloadJob job = jobBuilder.build();
        jobRepository.save(job);
        DownloadJob retrievedJob = jobRepository.findById(job.getId()).get();
        Assertions.assertEquals(job.getId(), retrievedJob.getId());
    }
}
