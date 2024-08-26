package org.uniprot.api.async.download.messaging.producer.map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.map.MapDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.map.MapMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.MapDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;

public abstract class MapProducerMessageServiceIT<T extends MapDownloadRequest>
        extends SolrProducerMessageServiceIT<T, MapDownloadJob> {
    @Autowired private MapDownloadJobRepository mapDownloadJobRepository;

    @Autowired private MapFileHandler mapFileHandler;

    @Autowired private MapDownloadConfigProperties mapDownloadConfigProperties;

    @MockBean private MapMessageConsumer mapMessageConsumer;

    @Override
    protected MessageConsumer getConsumer() {
        return mapMessageConsumer;
    }

    @Override
    protected DownloadJobRepository<MapDownloadJob> getMapDownloadJobRepository() {
        return mapDownloadJobRepository;
    }

    @Override
    protected FileHandler getMapFileHandler() {
        return mapFileHandler;
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return mapDownloadConfigProperties;
    }

    @TestConfiguration
    @ComponentScan({
        "org.uniprot.api.async.download.mq.map",
        "org.uniprot.api.async.download.service.map",
        "org.uniprot.api.async.download.messaging.consumer.map",
        "org.uniprot.api.async.download.messaging.config.map",
        "org.uniprot.api.async.download.messaging.result.map",
        "org.uniprot.api.async.download.messaging.consumer.heartbeat.map",
        "org.uniprot.api.async.download.messaging.producer.map"
    })
    static class MapProducerTestConfig {}
}
