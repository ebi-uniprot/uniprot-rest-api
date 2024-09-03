package org.uniprot.api.async.download.messaging.producer.map;

import java.time.LocalDateTime;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.config.map.MapDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.map.MapRabbitMQConfig;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
            MapProducerMessageServiceIT.MapProducerTestConfig.class,
            MapRabbitMQConfig.class,
            RedisConfigTest.class
        })
@EnableConfigurationProperties({MapDownloadConfigProperties.class})
public class UniProtKBMapProducerMessageServiceIT
        extends MapProducerMessageServiceIT<UniProtKBToUniRefMapDownloadRequest> {
    @Autowired private UniProtKBMapProducerMessageService service;

    @Override
    protected MapDownloadJob getDownloadJob(
            String jobId, LocalDateTime idleSince, UniProtKBToUniRefMapDownloadRequest request) {
        return new MapDownloadJob(
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
    protected UniProtKBToUniRefMapDownloadRequest getSuccessDownloadRequest() {
        UniProtKBToUniRefMapDownloadRequest request = new UniProtKBToUniRefMapDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        return request;
    }

    @Override
    protected UniProtKBToUniRefMapDownloadRequest getSuccessDownloadRequestWithForce() {
        UniProtKBToUniRefMapDownloadRequest request = new UniProtKBToUniRefMapDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        return request;
    }

    @Override
    protected UniProtKBToUniRefMapDownloadRequest getAlreadyRunningRequest() {
        UniProtKBToUniRefMapDownloadRequest request = new UniProtKBToUniRefMapDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        return request;
    }

    @Override
    protected UniProtKBToUniRefMapDownloadRequest getWithoutFormatRequest() {
        UniProtKBToUniRefMapDownloadRequest request = new UniProtKBToUniRefMapDownloadRequest();
        request.setQuery("Not using format");
        return request;
    }

    @Override
    protected SolrProducerMessageService<UniProtKBToUniRefMapDownloadRequest, MapDownloadJob>
            getService() {
        return service;
    }
}
