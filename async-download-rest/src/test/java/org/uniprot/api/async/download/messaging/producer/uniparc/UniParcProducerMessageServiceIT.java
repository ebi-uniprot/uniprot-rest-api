package org.uniprot.api.async.download.messaging.producer.uniparc;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcRabbitMQConfig;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.uniparc.UniParcMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.producer.uniparc.UniParcProducerMessageService;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniParcDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

import java.time.LocalDateTime;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
            UniParcProducerMessageServiceIT.UniParcProducerTestConfig.class,
            UniParcRabbitMQConfig.class,
            RedisConfigTest.class
        })
@EnableConfigurationProperties({UniParcDownloadConfigProperties.class})
public class UniParcProducerMessageServiceIT
        extends SolrProducerMessageServiceIT<UniParcDownloadRequest, UniParcDownloadJob> {

    @Autowired private UniParcProducerMessageService service;

    @Autowired private UniParcDownloadJobRepository jobRepository;

    @Autowired private UniParcFileHandler fileHandler;

    @Autowired private UniParcDownloadConfigProperties uniParcDownloadConfigProperties;

    @MockBean private UniParcMessageConsumer uniParcConsumer;

    @Override
    protected UniParcDownloadJob getDownloadJob(
            String jobId, LocalDateTime idleSince, UniParcDownloadRequest request) {
        return new UniParcDownloadJob(
                jobId,
                JobStatus.RUNNING,
                null,
                idleSince,
                null,
                0,
                request.getQuery(),
                request.getFields(),
                request.getSort(),
                null,
                request.getFormat(),
                100,
                10,
                1);
    }

    @Override
    protected DownloadJobRepository<UniParcDownloadJob> getJobRepository() {
        return jobRepository;
    }

    @Override
    protected MessageConsumer<UniParcDownloadRequest, UniParcDownloadJob> getConsumer() {
        return uniParcConsumer;
    }

    @Override
    protected UniParcDownloadRequest getSuccessDownloadRequest() {
        UniParcDownloadRequest request = new UniParcDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        return request;
    }

    @Override
    protected UniParcDownloadRequest getSuccessDownloadRequestWithForce() {
        UniParcDownloadRequest request = new UniParcDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        return request;
    }

    @Override
    protected UniParcDownloadRequest getAlreadyRunningRequest() {
        UniParcDownloadRequest request = new UniParcDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        return request;
    }

    @Override
    protected UniParcDownloadRequest getWithoutFormatRequest() {
        UniParcDownloadRequest request = new UniParcDownloadRequest();
        request.setQuery("Not using format");
        return request;
    }

    @Override
    protected SolrProducerMessageService<UniParcDownloadRequest, UniParcDownloadJob> getService() {
        return service;
    }

    @Override
    protected FileHandler getFileHandler() {
        return fileHandler;
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return uniParcDownloadConfigProperties;
    }

    @TestConfiguration
    @ComponentScan({
        "org.uniprot.api.async.download.messaging.producer.uniparc",
        "org.uniprot.api.async.download.mq.uniparc",
        "org.uniprot.api.async.download.service.uniparc",
        "org.uniprot.api.async.download.messaging.consumer.uniparc",
        "org.uniprot.api.async.download.messaging.config.uniparc",
        "org.uniprot.api.async.download.messaging.result.uniparc",
        "org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc",
        "org.uniprot.api.async.download.messaging.producer.uniparc"
    })
    static class UniParcProducerTestConfig {}
}
