package org.uniprot.api.async.download.messaging.consumer.processor.mapto.to;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;

import net.jodah.failsafe.RetryPolicy;

@Component
public class UniRefMapToIdRequestProcessor
        extends MapToIdRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    private final UniRefEntryLightService uniRefEntryLightService;
    private final RetryPolicy<Object> retryPolicy;
    private final int batchSize;

    protected UniRefMapToIdRequestProcessor(
            MapToFileHandler fileHandler,
            MapToJobService jobService,
            UniRefEntryLightService uniRefEntryLightService) {
        super(fileHandler, jobService);
        this.uniRefEntryLightService = uniRefEntryLightService;
        this.retryPolicy =
                new RetryPolicy<>()
                        .handle(Exception.class)
                        .withMaxRetries(3)
                        .withDelay(Duration.ofMillis(10));
        this.batchSize = 100;
    }

    @Override
    protected long getSolrHits(Stream<String> ids) {
        UniRefBatchEntryIterable uniRefBatchEntryIterable =
                new UniRefBatchEntryIterable(
                        ids.iterator(), uniRefEntryLightService, retryPolicy, batchSize);
        return StreamSupport.stream(uniRefBatchEntryIterable.spliterator(), false).flatMap(Collection::stream).mapToLong(Long::longValue).sum();
    }

    @Override
    protected Stream<String> mapIds(Stream<String> ids) {
        UniRefBatchIdIterable uniRefBatchIdIterable =
                new UniRefBatchIdIterable(
                        ids.iterator(), uniRefEntryLightService, retryPolicy, batchSize);
        return StreamSupport.stream(uniRefBatchIdIterable.spliterator(), false)
                .flatMap(Collection::stream);
    }
}
