package org.uniprot.api.rest.download;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.request.StreamRequest;

@Slf4j
public abstract class AbstractDownloadResultWriter<T> implements DownloadResultWriter {

    private final List<HttpMessageConverter<?>> messageConverters;
    private final MessageConverterContextFactory<T> converterContextFactory;
    protected final StoreStreamerConfig<T> storeStreamerConfig;
    private final DownloadConfigProperties downloadConfigProperties;
    private final MessageConverterContextFactory.Resource resource;

    public AbstractDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<T> converterContextFactory,
            StoreStreamerConfig<T> storeStreamerConfig,
            DownloadConfigProperties downloadConfigProperties,
            MessageConverterContextFactory.Resource resource) {
        this.messageConverters = contentAdapter.getMessageConverters();
        this.converterContextFactory = converterContextFactory;
        this.storeStreamerConfig = storeStreamerConfig;
        this.downloadConfigProperties = downloadConfigProperties;
        this.resource = resource;
    }

    public void writeResult(
            StreamRequest request,
            Path idFile,
            String jobId,
            MediaType contentType,
            StoreRequest storeRequest)
            throws IOException {
        Path resultPath = Paths.get(downloadConfigProperties.getResultFilesFolder(), jobId);
        AbstractUUWHttpMessageConverter<T, T> outputWriter =
                getOutputWriter(contentType, getType());
        try (Stream<String> ids = Files.lines(idFile);
                OutputStream output =
                        Files.newOutputStream(
                                resultPath,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING)) {
            BatchStoreIterable<T> batchStoreIterable =
                    getBatchStoreIterable(ids.iterator(), storeRequest);
            Stream<T> entities =
                    StreamSupport.stream(batchStoreIterable.spliterator(), false)
                            .flatMap(Collection::stream)
                            .onClose(() -> log.debug("Finished streaming entries."));

            MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
            context.setFields(request.getFields());
            context.setContentType(contentType);
            context.setEntities(entities);

            Instant start = Instant.now();
            AtomicInteger counter = new AtomicInteger();
            outputWriter.writeContents(context, output, start, counter);
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