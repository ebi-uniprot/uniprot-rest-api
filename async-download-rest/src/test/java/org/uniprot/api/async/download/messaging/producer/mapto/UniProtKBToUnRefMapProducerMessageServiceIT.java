package org.uniprot.api.async.download.messaging.producer.mapto;

import java.time.LocalDateTime;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.config.mapto.MapToDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.mapto.MapToRabbitMQConfig;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
            MapToProducerMessageServiceIT.MapProducerTestConfig.class,
            MapToRabbitMQConfig.class,
            RedisConfigTest.class
        })
@EnableConfigurationProperties({MapToDownloadConfigProperties.class})
public class UniProtKBToUnRefMapProducerMessageServiceIT
        extends MapToProducerMessageServiceIT<UniProtKBToUniRefDownloadRequest> {
    @Autowired private UniProtKBToUniRefProducerMessageService service;

    @Override
    protected MapToDownloadJob getDownloadJob(
            String jobId, LocalDateTime idleSince, UniProtKBToUniRefDownloadRequest request) {
        return new MapToDownloadJob(
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
    protected UniProtKBToUniRefDownloadRequest getSuccessDownloadRequest() {
        UniProtKBToUniRefDownloadRequest request = new UniProtKBToUniRefDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        return request;
    }

    @Override
    protected UniProtKBToUniRefDownloadRequest getSuccessDownloadRequestWithForce() {
        UniProtKBToUniRefDownloadRequest request = new UniProtKBToUniRefDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        return request;
    }

    @Override
    protected UniProtKBToUniRefDownloadRequest getAlreadyRunningRequest() {
        UniProtKBToUniRefDownloadRequest request = new UniProtKBToUniRefDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        return request;
    }

    @Override
    protected UniProtKBToUniRefDownloadRequest getWithoutFormatRequest() {
        UniProtKBToUniRefDownloadRequest request = new UniProtKBToUniRefDownloadRequest();
        request.setQuery("Not using format");
        return request;
    }

    @Override
    protected SolrProducerMessageService<UniProtKBToUniRefDownloadRequest, MapToDownloadJob>
            getService() {
        return service;
    }

    @Override
    protected @NotNull String getJobHashForWithoutFormatDefaultToJson() {
        return "964a9c6154896d88132b6082d397dc6ca9d9f733";
    }

    @Override
    protected @NotNull String getJobHashForSuccess() {
        return "e01159046a85cbd920622f34511d9e4c198c6ca4";
    }

    @Override
    protected @NotNull String getJobHashForSuccessForceAndIdleJobAllowedAndCleanResources() {
        return "7a5b14e8b59dd20dcbd27ba8cad1fb45c4806575";
    }

    @Override
    protected @NotNull String getJobHashForAlreadyRunning() {
        return "ab3f9709a8a59b7241901f13167686bf124fdd81";
    }
}
