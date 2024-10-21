package org.uniprot.api.async.download.messaging.producer.mapto;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.mapto.MapToDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.mapto.MapToMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.MapToDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;

public abstract class MapToProducerMessageServiceIT<T extends MapToDownloadRequest>
        extends SolrProducerMessageServiceIT<T, MapToDownloadJob> {
    @Autowired private MapToDownloadJobRepository mapDownloadJobRepository;

    @Autowired private MapToFileHandler mapToFileHandler;

    @Autowired private MapToDownloadConfigProperties mapToDownloadConfigProperties;

    @MockBean private MapToMessageConsumer mapToMessageConsumer;

    @Override
    protected MessageConsumer getConsumer() {
        return mapToMessageConsumer;
    }

    @Override
    protected DownloadJobRepository<MapToDownloadJob> getMapDownloadJobRepository() {
        return mapDownloadJobRepository;
    }

    @Override
    protected FileHandler getMapFileHandler() {
        return mapToFileHandler;
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return mapToDownloadConfigProperties;
    }

    @TestConfiguration
    @ComponentScan({
        "org.uniprot.api.async.download.mq.mapto",
        "org.uniprot.api.async.download.service.mapto",
        "org.uniprot.api.async.download.messaging.consumer.mapto",
        "org.uniprot.api.async.download.messaging.config.mapto",
        "org.uniprot.api.async.download.messaging.result.mapto",
        "org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto",
        "org.uniprot.api.async.download.messaging.producer.mapto"
    })
    static class MapProducerTestConfig {}

    @Override
    protected @NotNull String getJobHashForSuccess() {
        return "e01159046a85cbd920622f34511d9e4c198c6ca4";
    }

    @Override
    protected @NotNull String getJobHashForSuccessForceAndIdleJobAllowedAndCleanResources() {
        return "7a5b14e8b59dd20dcbd27ba8cad1fb45c4806575";
    }

    @Override
    protected @NotNull String getJobHashForWithoutFormatDefaultToJson() {
        return "964a9c6154896d88132b6082d397dc6ca9d9f733";
    }

    @Override
    protected @NotNull String getJobHashForAlreadyRunning() {
        return "ab3f9709a8a59b7241901f13167686bf124fdd81";
    }
}
