package org.uniprot.api.async.download.messaging.producer.uniref;

import java.time.LocalDateTime;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitMQConfig;
import org.uniprot.api.async.download.messaging.consumer.ContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.uniref.UniRefContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.rest.download.model.JobStatus;

@ExtendWith(SpringExtension.class)
@Import({
    UniRefProducerMessageServiceIT.UniRefProducerTestConfig.class,
    UniRefRabbitMQConfig.class,
    RedisConfigTest.class
})
@EnableConfigurationProperties({UniRefDownloadConfigProperties.class})
public class UniRefProducerMessageServiceIT
        extends SolrProducerMessageServiceIT<UniRefDownloadRequest, UniRefDownloadJob> {

    @Autowired private UniRefProducerMessageService service;

    @Autowired private UniRefDownloadJobRepository jobRepository;

    @Autowired private UniRefAsyncDownloadFileHandler fileHandler;

    @Autowired private UniRefJobService uniRefJobService;

    @Autowired private UniRefAsyncDownloadSubmissionRules uniRefAsyncDownloadSubmissionRules;

    @Autowired private UniRefDownloadConfigProperties uniRefDownloadConfigProperties;

    @MockBean private UniRefContentBasedAndRetriableMessageConsumer uniRefConsumer;

    @MockBean private UniProtKBHeartbeatProducer heartBeat;

    @Override
    protected UniRefDownloadJob getDownloadJob(
            String jobId, LocalDateTime idleSince, UniRefDownloadRequest request) {
        return new UniRefDownloadJob(
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
    protected DownloadJobRepository<UniRefDownloadJob> getJobRepository() {
        return jobRepository;
    }

    @Override
    protected ContentBasedAndRetriableMessageConsumer<UniRefDownloadRequest, UniRefDownloadJob>
            getConsumer() {
        return uniRefConsumer;
    }

    @Override
    protected UniRefDownloadRequest getSuccessDownloadRequest() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        return request;
    }

    @Override
    protected UniRefDownloadRequest getSuccessDownloadRequestWithForce() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        return request;
    }

    @Override
    protected UniRefDownloadRequest getAlreadyRunningRequest() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        return request;
    }

    @Override
    protected UniRefDownloadRequest getWithoutFormatRequest() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("Not using format");
        return request;
    }

    @Override
    protected SolrProducerMessageService<UniRefDownloadRequest, UniRefDownloadJob> getService() {
        return service;
    }

    @Override
    protected AsyncDownloadFileHandler getFileHandler() {
        return fileHandler;
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return uniRefDownloadConfigProperties;
    }

    @TestConfiguration
    @ComponentScan({
        "org.uniprot.api.async.download.messaging.producer.uniref",
        "org.uniprot.api.async.download.mq.uniref",
        "org.uniprot.api.async.download.service.uniref",
        "org.uniprot.api.async.download.messaging.consumer.uniref",
        "org.uniprot.api.async.download.messaging.config.uniref",
        "org.uniprot.api.async.download.messaging.result.uniref",
        "org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref",
        "org.uniprot.api.async.download.messaging.producer.uniref"
    })
    static class UniRefProducerTestConfig {}
}
