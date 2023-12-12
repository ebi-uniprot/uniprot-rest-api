package org.uniprot.api.idmapping.queue;

import static org.uniprot.api.idmapping.service.IdMappingServiceUtils.getExtraOptions;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES;

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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.rest.download.heartbeat.HeartBeatProducer;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;

@Slf4j
public abstract class AbstractIdMappingDownloadResultWriter<T extends EntryPair<S>, S> {

    private final List<HttpMessageConverter<?>> messageConverters;
    private final MessageConverterContextFactory<T> converterContextFactory;
    protected final StoreStreamerConfig<S> storeStreamerConfig;
    private final DownloadConfigProperties downloadConfigProperties;
    private final MessageConverterContextFactory.Resource resource;
    private final RdfStreamer rdfStreamer;
    private final HeartBeatProducer heartBeatProducer;

    protected AbstractIdMappingDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<T> converterContextFactory,
            StoreStreamerConfig<S> storeStreamerConfig,
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
            IdMappingDownloadRequest request,
            IdMappingResult idMappingResult,
            DownloadJob downloadJob,
            MediaType contentType)
            throws IOException {
        String jobId = downloadJob.getId();
        String fileNameWithExt = jobId + FileType.GZIP.getExtension();
        Path resultPath =
                Paths.get(downloadConfigProperties.getResultFilesFolder(), fileNameWithExt);
        AbstractUUWHttpMessageConverter<T, S> outputWriter =
                getOutputWriter(contentType, getType());
        try (OutputStream output =
                        Files.newOutputStream(
                                resultPath,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output)) {

            ExtraOptions extraOptions = getExtraOptions(idMappingResult);

            MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
            context.setFields(request.getFields());
            context.setContentType(contentType);
            context.setExtraOptions(extraOptions);
            context.setWarnings(idMappingResult.getWarnings());

            if (SUPPORTED_RDF_MEDIA_TYPES.containsKey(contentType)) {
                Set<String> toIds = getToIds(idMappingResult);
                Stream<String> rdfResponse =
                        this.rdfStreamer.stream(
                                toIds.stream(),
                                resource.name().toLowerCase(),
                                SUPPORTED_RDF_MEDIA_TYPES.get(contentType),
                                entries ->
                                        heartBeatProducer.createWithProgress(downloadJob, entries));
                context.setEntityIds(rdfResponse);
            } else if (contentType.equals(LIST_MEDIA_TYPE)) {
                Set<String> toIds = getToIds(idMappingResult);
                context.setEntityIds(
                        toIds.stream()
                                .map(
                                        id -> {
                                            heartBeatProducer.createWithProgress(downloadJob, 1);
                                            return id;
                                        }));
            } else {
                BatchStoreEntryPairIterable<T, S> batchStoreIterable =
                        getBatchStoreEntryPairIterable(
                                idMappingResult.getMappedIds().iterator(), request.getFields());
                Stream<T> entities =
                        StreamSupport.stream(batchStoreIterable.spliterator(), false)
                                .map(
                                        entityCollection -> {
                                            heartBeatProducer.createWithProgress(
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
            }
            Instant start = Instant.now();
            AtomicInteger counter = new AtomicInteger();
            outputWriter.writeContents(context, gzipOutputStream, start, counter);
        } finally {
            heartBeatProducer.stop(downloadJob.getId());
        }
    }

    private Set<String> getToIds(IdMappingResult idMappingResult) {
        return idMappingResult.getMappedIds().stream()
                .map(IdMappingStringPair::getTo)
                .collect(Collectors.toSet());
    }

    private AbstractUUWHttpMessageConverter<T, S> getOutputWriter(
            MediaType contentType, Type type) {
        return messageConverters.stream()
                .filter(AbstractUUWHttpMessageConverter.class::isInstance)
                .filter(
                        converter ->
                                ((AbstractUUWHttpMessageConverter<?, ?>) converter)
                                        .canWrite(type, MessageConverterContext.class, contentType))
                .map(converter -> (AbstractUUWHttpMessageConverter<T, S>) converter)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Unable to find Message converter"));
    }

    public abstract BatchStoreEntryPairIterable<T, S> getBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> idsIterator, String fields);

    public abstract Type getType();
}
