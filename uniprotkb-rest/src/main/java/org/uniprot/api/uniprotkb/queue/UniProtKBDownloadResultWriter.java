package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterable;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
@Slf4j
public class UniProtKBDownloadResultWriter implements DownloadResultWriter {

    private final List<HttpMessageConverter<?>> messageConverters;
    private final MessageConverterContextFactory<UniProtKBEntry> converterContextFactory;
    protected final StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig;
    protected final TaxonomyLineageService lineageService;
    private final MessageConverterContextFactory.Resource resource;

    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntry>>() {})
                    .getType();

    public UniProtKBDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig,
            TaxonomyLineageService lineageService) {
        this.messageConverters = contentAdapter.getMessageConverters();
        this.converterContextFactory = converterContextFactory;
        this.storeStreamerConfig = storeStreamerConfig;
        this.lineageService = lineageService;
        this.resource = MessageConverterContextFactory.Resource.UNIPROTKB;
    }

    public void writeResult(
            StreamRequest request,
            Path idFile,
            String jobId,
            MediaType contentType,
            StoreRequest storeRequest)
            throws IOException {
        Path resultPath = Paths.get("/tmp/downloadOutput", jobId);
        AbstractUUWHttpMessageConverter<UniProtKBEntry, UniProtKBEntry> outputWriter =
                getOutputWriter(contentType, type);
        try (Stream<String> ids = Files.lines(idFile);
                OutputStream output =
                        Files.newOutputStream(
                                resultPath,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING)) {
            BatchStoreIterable<UniProtKBEntry> batchStoreIterable =
                    getBatchStoreIterable(ids.iterator(), storeRequest);
            Stream<UniProtKBEntry> entities =
                    StreamSupport.stream(batchStoreIterable.spliterator(), false)
                            .flatMap(Collection::stream)
                            .onClose(() -> log.debug("Finished streaming entries."));

            MessageConverterContext<UniProtKBEntry> context =
                    converterContextFactory.get(resource, contentType);
            context.setFields(request.getFields());
            context.setContentType(contentType);
            context.setEntities(entities);

            Instant start = Instant.now();
            AtomicInteger counter = new AtomicInteger();
            outputWriter.writeContents(context, output, start, counter);
        }
    }

    private AbstractUUWHttpMessageConverter<UniProtKBEntry, UniProtKBEntry> getOutputWriter(
            MediaType contentType, Type type) {
        return (AbstractUUWHttpMessageConverter<UniProtKBEntry, UniProtKBEntry>)
                messageConverters.stream()
                        .filter(c -> c instanceof AbstractUUWHttpMessageConverter)
                        .filter(
                                c ->
                                        ((GenericHttpMessageConverter<?>) c)
                                                .canWrite(
                                                        type,
                                                        MessageConverterContext.class,
                                                        contentType))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unable to find Message converter"));
    }

    private BatchStoreIterable<UniProtKBEntry> getBatchStoreIterable(
            Iterator<String> idsIterator, StoreRequest storeRequest) {
        return new UniProtKBBatchStoreIterable(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                lineageService,
                storeRequest.isAddLineage());
    }
}
