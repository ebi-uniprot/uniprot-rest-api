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
        return "7df83695b95d9ff02f48227ef77d433da524e29f";
    }

    @Override
    protected @NotNull String getJobHashForSuccessForceAndIdleJobAllowedAndCleanResources() {
        return "394c4291a376a0abcc0970e98df9e9a508b08097";
    }

    @Override
    protected @NotNull String getJobHashForWithoutFormatDefaultToJson() {
        return "5109e0bb83845063663bd7e6862610269490f298";
    }

    @Override
    protected @NotNull String getJobHashForAlreadyRunning() {
        return "5762160d06e543cf019fad87a720d8676d5410d6";
    }
}
