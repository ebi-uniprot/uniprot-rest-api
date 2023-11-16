package org.uniprot.api.rest.download;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.request.DownloadRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPOutputStream;

import static org.uniprot.api.rest.output.UniProtMediaType.*;

@Slf4j
public abstract class AbstractDownloadResultWriter<T> implements DownloadResultWriter {

    private static final Map<MediaType, String> SUPPORTED_RDF_TYPES =
            Map.of(
                    RDF_MEDIA_TYPE, "rdf",
                    TURTLE_MEDIA_TYPE, "ttl",
                    N_TRIPLES_MEDIA_TYPE, "nt");
    private final List<HttpMessageConverter<?>> messageConverters;
    private final MessageConverterContextFactory<T> converterContextFactory;
    protected final StoreStreamerConfig<T> storeStreamerConfig;
    private final DownloadConfigProperties downloadConfigProperties;
    private final DownloadJobRepository jobRepository;
    private final MessageConverterContextFactory.Resource resource;
    private final RdfStreamer rdfStreamer;
    private final AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    private final Map<String, Long> downloadJobCheckPoints = new HashMap<>();

    protected AbstractDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<T> converterContextFactory,
            StoreStreamerConfig<T> storeStreamerConfig,
            DownloadConfigProperties downloadConfigProperties,
            RdfStreamer rdfStreamer,
            DownloadJobRepository jobRepository,
            MessageConverterContextFactory.Resource resource,
            AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration) {
        this.messageConverters = contentAdapter.getMessageConverters();
        this.converterContextFactory = converterContextFactory;
        this.storeStreamerConfig = storeStreamerConfig;
        this.downloadConfigProperties = downloadConfigProperties;
        this.jobRepository = jobRepository;
        this.resource = resource;
        this.rdfStreamer = rdfStreamer;
        this.asyncDownloadHeartBeatConfiguration = asyncDownloadHeartBeatConfiguration;
    }

    public void writeResult(
            DownloadRequest request,
            DownloadJob downloadJob,
            Path idFile,
            MediaType contentType,
            StoreRequest storeRequest,
            String dataType)
            throws IOException {
        String jobId = downloadJob.getId();
        String fileNameWithExt = jobId + FileType.GZIP.getExtension();
        Path resultPath =
                Paths.get(downloadConfigProperties.getResultFilesFolder(), fileNameWithExt);
        AbstractUUWHttpMessageConverter<T, T> outputWriter =
                getOutputWriter(contentType, getType());
        try (Stream<String> ids = Files.lines(idFile);
             OutputStream output =
                     Files.newOutputStream(
                             resultPath,
                             StandardOpenOption.WRITE,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output)) {

            MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
            context.setFields(request.getFields());
            context.setContentType(contentType);

            if (SUPPORTED_RDF_TYPES.containsKey(contentType)) {
                Stream<String> rdfResponse =
                        this.rdfStreamer.stream(
                                ids,
                                dataType,
                                SUPPORTED_RDF_TYPES.get(contentType),
                                entries -> updateEntriesProcessed(downloadJob, entries));
                context.setEntityIds(rdfResponse);
                downloadJobCheckPoints.remove(downloadJob.getId());
            } else if (contentType.equals(LIST_MEDIA_TYPE)) {
                context.setEntityIds(
                        ids.map(
                                id -> {
                                    updateEntriesProcessed(downloadJob, 1);
                                    return id;
                                }));
                downloadJobCheckPoints.remove(downloadJob.getId());
            } else {
                BatchStoreIterable<T> batchStoreIterable =
                        getBatchStoreIterable(ids.iterator(), storeRequest);
                Stream<T> entities =
                        StreamSupport.stream(batchStoreIterable.spliterator(), false)
                                .map(
                                        entityCollection -> {
                                            updateEntriesProcessed(
                                                    downloadJob, entityCollection.size());
                                            return entityCollection;
                                        })
                                .flatMap(Collection::stream)
                                .onClose(
                                        () ->
                                                log.info(
                                                        "Finished streaming entries for job {}",
                                                        jobId));

                context.setEntities(entities);
                downloadJobCheckPoints.remove(downloadJob.getId());
            }
            Instant start = Instant.now();
            AtomicInteger counter = new AtomicInteger();
            outputWriter.writeContents(context, gzipOutputStream, start, counter);
        }
    }

    private void updateEntriesProcessed(DownloadJob downloadJob, long size) {
        try {
            if (asyncDownloadHeartBeatConfiguration.isEnabled()) {
                String jobId = downloadJob.getId();
                long totalNumberOfProcessedEntries = downloadJobCheckPoints.getOrDefault(jobId, 0L) + size;
                downloadJobCheckPoints.put(jobId, totalNumberOfProcessedEntries);
                if (isNextCheckPointPassed(downloadJob, totalNumberOfProcessedEntries)) {
                    downloadJob.setEntriesProcessed(totalNumberOfProcessedEntries);
                    downloadJob.setUpdated(LocalDateTime.now());
                    jobRepository.save(downloadJob);
                }
            }
        } catch (Exception e) {
            log.warn(
                    String.format(
                            "Updating the Number of Processed Entries was failed for Download Job ID: %s , "
                                    + "Last updated number of entries processed: %d",
                            downloadJob.getId(), downloadJob.getEntriesProcessed()));
        }
    }

    private boolean isNextCheckPointPassed(
            DownloadJob downloadJob, long totalNumberOfProcessedEntries) {
        long nextCheckPoint =
                downloadJob.getEntriesProcessed()
                        + asyncDownloadHeartBeatConfiguration.getInterval();
        long totalNumberOfEntries = downloadJob.getTotalEntries();
        return totalNumberOfProcessedEntries >= Math.min(totalNumberOfEntries, nextCheckPoint);
    }

    private AbstractUUWHttpMessageConverter<T, T> getOutputWriter(
            MediaType contentType, Type type) {
        return messageConverters.stream()
                .filter(converter -> converter instanceof AbstractUUWHttpMessageConverter)
                .filter(
                        converter ->
                                ((AbstractUUWHttpMessageConverter<?, ?>) converter)
                                        .canWrite(type, MessageConverterContext.class, contentType))
                .map(converter -> (AbstractUUWHttpMessageConverter<T, T>) converter)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Unable to find Message converter"));
    }

    public abstract BatchStoreIterable<T> getBatchStoreIterable(
            Iterator<String> idsIterator, StoreRequest storeRequest);

    public abstract Type getType();
}
