package org.uniprot.api.async.download.messaging.producer.uniprotkb;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitMQConfig;
import org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.uniprotkb.UniProtKBContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

import java.time.LocalDateTime;

@ExtendWith(SpringExtension.class)
@Import({UniProtKBProducerMessageServiceIT.UniProtKBProducerTestConfig.class, UniProtKBRabbitMQConfig.class, RedisConfigTest.class})
@EnableConfigurationProperties({UniProtKBDownloadConfigProperties.class})
class UniProtKBProducerMessageServiceIT extends SolrProducerMessageServiceIT<UniProtKBDownloadRequest, UniProtKBDownloadJob> {

    @Autowired
    private UniProtKBProducerMessageService service;

    @Autowired
    UniProtKBDownloadJobRepository jobRepository;

    @Autowired
    UniProtKBAsyncDownloadFileHandler fileHandler;

    @Autowired
    UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties;

    @MockBean
    private UniProtKBContentBasedAndRetriableMessageConsumer uniProtKBConsumer;

    @Override
    protected UniProtKBDownloadJob getDownloadJob(String jobId, LocalDateTime idleSince, UniProtKBDownloadRequest request) {
        return new UniProtKBDownloadJob(jobId, JobStatus.RUNNING, null, idleSince, null, 0, request.getQuery(), request.getFields(), request.getSort(), null, request.getFormat(), 100, 10, 1);
    }

    @Override
    protected DownloadJobRepository<UniProtKBDownloadJob> getJobRepository() {
        return jobRepository;
    }

    @Override
    protected ContentBasedAndRetriableMessageConsumer<UniProtKBDownloadRequest, UniProtKBDownloadJob> getConsumer() {
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
    protected UniProtKBDownloadRequest getSuccessDownloadRequestWithForce(){
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        return request;
    }

    @Override
    protected SolrProducerMessageService<UniProtKBDownloadRequest, UniProtKBDownloadJob> getService() {
        return service;
    }

    @Override
    protected AsyncDownloadFileHandler getFileHandler() {
        return fileHandler;
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return uniProtKBDownloadConfigProperties;
    }

    @TestConfiguration
    @ComponentScan({"org.uniprot.api.async.download.messaging.producer.uniprotkb",
            "org.uniprot.api.async.download.refactor.mq.uniprotkb",
            "org.uniprot.api.async.download.refactor.service.uniprotkb",
            "org.uniprot.api.async.download.messaging.consumer.uniprotkb",
            "org.uniprot.api.async.download.messaging.config.uniprotkb",
            "org.uniprot.api.async.download.messaging.result.uniprotkb",
            "org.uniprot.api.async.download.messaging.consumer.common",
            "org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb",
            "org.uniprot.api.async.download.messaging.producer.uniprotkb"})
    static class UniProtKBProducerTestConfig {
    }
}
