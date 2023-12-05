package org.uniprot.api.rest.download;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.rest.download.heartbeat.HeartBeatProducer;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final MessageConverterContextFactory.Resource resource;
    private final RdfStreamer rdfStreamer;
    private final HeartBeatProducer heartBeatProducer;

    protected AbstractDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<T> converterContextFactory,
            StoreStreamerConfig<T> storeStreamerConfig,
            DownloadConfigProperties downloadConfigProperties,
            RdfStreamer rdfStreamer,
            MessageConverterContextFactory.Resource resource,
            HeartBeatProducer heartBeatProducer) {
        this.messageConverters = contentAdapter.getMessageConverters();
        this.converterContextFactory = converterContextFactory;
        this.storeStreamerConfig = storeStreamerConfig;
        this.downloadConfigProperties = downloadConfigProperties;
        this.resource = resource;
        this.rdfStreamer = rdfStreamer;
        this.heartBeatProducer = heartBeatProducer;
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
                                entries ->
                                        heartBeatProducer.updateEntriesProcessed(
                                                downloadJob, entries));
                context.setEntityIds(rdfResponse);
                heartBeatProducer.stopHeartBeat(downloadJob.getId());
            } else if (contentType.equals(LIST_MEDIA_TYPE)) {
                context.setEntityIds(
                        ids.map(
                                id -> {
                                    heartBeatProducer.updateEntriesProcessed(downloadJob, 1);
                                    return id;
                                }));
                heartBeatProducer.stopHeartBeat(downloadJob.getId());
            } else {
                BatchStoreIterable<T> batchStoreIterable =
                        getBatchStoreIterable(ids.iterator(), storeRequest);
                Stream<T> entities =
                        StreamSupport.stream(batchStoreIterable.spliterator(), false)
                                .map(
                                        entityCollection -> {
                                            heartBeatProducer.updateEntriesProcessed(
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
                heartBeatProducer.stopHeartBeat(downloadJob.getId());
            }
            Instant start = Instant.now();
            AtomicInteger counter = new AtomicInteger();
            outputWriter.writeContents(context, gzipOutputStream, start, counter);
        }
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
