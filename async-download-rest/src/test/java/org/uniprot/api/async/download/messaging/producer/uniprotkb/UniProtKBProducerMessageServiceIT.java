package org.uniprot.api.async.download.messaging.producer.uniprotkb;

import java.time.LocalDateTime;

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
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitMQConfig;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.uniprotkb.UniProtKBMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
            UniProtKBProducerMessageServiceIT.UniProtKBProducerTestConfig.class,
            UniProtKBRabbitMQConfig.class,
            RedisConfigTest.class
        })
@EnableConfigurationProperties({UniProtKBDownloadConfigProperties.class})
class UniProtKBProducerMessageServiceIT
        extends SolrProducerMessageServiceIT<UniProtKBDownloadRequest, UniProtKBDownloadJob> {

    @Autowired private UniProtKBProducerMessageService service;

    @Autowired private UniProtKBDownloadJobRepository jobRepository;

    @Autowired private UniProtKBFileHandler fileHandler;

    @Autowired private UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties;

    @MockBean private UniProtKBMessageConsumer uniProtKBConsumer;

    @Override
    protected UniProtKBDownloadJob getDownloadJob(
            String jobId, LocalDateTime idleSince, UniProtKBDownloadRequest request) {
        return new UniProtKBDownloadJob(
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
    protected DownloadJobRepository<UniProtKBDownloadJob> getMapDownloadJobRepository() {
        return jobRepository;
    }

    @Override
    protected MessageConsumer<UniProtKBDownloadRequest, UniProtKBDownloadJob> getConsumer() {
        return uniProtKBConsumer;
    }

    @Override
    protected UniProtKBDownloadRequest getSuccessDownloadRequest() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        return request;
    }

    @Override
    protected UniProtKBDownloadRequest getAlreadyRunningRequest() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        return request;
    }

    @Override
    protected UniProtKBDownloadRequest getWithoutFormatRequest() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("Not using format");
        return request;
    }

    @Override
    protected UniProtKBDownloadRequest getSuccessDownloadRequestWithForce() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        return request;
    }

    @Override
    protected SolrProducerMessageService<UniProtKBDownloadRequest, UniProtKBDownloadJob>
            getService() {
        return service;
    }

    @Override
    protected FileHandler getMapFileHandler() {
        return fileHandler;
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return uniProtKBDownloadConfigProperties;
    }

    @TestConfiguration
    @ComponentScan({
        "org.uniprot.api.async.download.messaging.producer.uniprotkb",
        "org.uniprot.api.async.download.mq.uniprotkb",
        "org.uniprot.api.async.download.service.uniprotkb",
        "org.uniprot.api.async.download.messaging.consumer.uniprotkb",
        "org.uniprot.api.async.download.messaging.config.uniprotkb",
        "org.uniprot.api.async.download.messaging.result.uniprotkb",
        "org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb",
        "org.uniprot.api.async.download.messaging.producer.uniprotkb"
    })
    static class UniProtKBProducerTestConfig {}
}
