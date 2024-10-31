package org.uniprot.api.async.download.messaging.consumer.processor.mapto.to;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.mapto.MapToAsyncDownloadToIdConfigProperties;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;

import net.jodah.failsafe.RetryPolicy;

@Component
public class UniProtKBToUniRefToIdRequestProcessor
        extends MapToIdRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    private final UniRefEntryLightService uniRefEntryLightService;
    private final RetryPolicy<Object> retryPolicy;
    private final MapToAsyncDownloadToIdConfigProperties mapToAsyncDownloadToIdConfigProperties;

    protected UniProtKBToUniRefToIdRequestProcessor(
            MapToFileHandler fileHandler,
            MapToJobService jobService,
            UniRefEntryLightService uniRefEntryLightService,
            MapToAsyncDownloadToIdConfigProperties mapToAsyncDownloadToIdConfigProperties) {
        super(fileHandler, jobService);
        this.uniRefEntryLightService = uniRefEntryLightService;
        this.mapToAsyncDownloadToIdConfigProperties = mapToAsyncDownloadToIdConfigProperties;
        this.retryPolicy =
                new RetryPolicy<>()
                        .handle(Exception.class)
                        .withMaxRetries(mapToAsyncDownloadToIdConfigProperties.getRetryMaxCount())
                        .withDelay(
                                Duration.ofMillis(
                                        mapToAsyncDownloadToIdConfigProperties
                                                .getWaitingMaxTime()));
    }

    @Override
    protected long getSolrHits(Stream<String> ids) {
        UniRefBatchEntryIterable uniRefBatchEntryIterable =
                new UniRefBatchEntryIterable(
                        ids.iterator(),
                        uniRefEntryLightService,
                        retryPolicy,
                        mapToAsyncDownloadToIdConfigProperties.getBatchSize());
        return StreamSupport.stream(uniRefBatchEntryIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .mapToLong(Long::longValue)
                .sum();
    }

    @Override
    protected Stream<String> mapIds(Stream<String> ids) {
        UniRefBatchIdIterable uniRefBatchIdIterable =
                new UniRefBatchIdIterable(
                        ids.iterator(),
                        uniRefEntryLightService,
                        retryPolicy,
                        mapToAsyncDownloadToIdConfigProperties.getBatchSize());
        return StreamSupport.stream(uniRefBatchIdIterable.spliterator(), false)
                .flatMap(Collection::stream);
    }
}
