package org.uniprot.api.async.download.messaging.consumer.streamer.facade;

import static org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer.SUPPORTED_RDF_TYPES;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.valueOf;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

public abstract class SolrIdResultStreamerFacade<
        T extends DownloadRequest, R extends DownloadJob, U> {
    private final RDFResultStreamer<T, R> rdfResultStreamer;
    private final ListResultStreamer<T, R> listResultStreamer;
    private final SolrIdBatchResultStreamer<T, R, U> solrIdBatchResultStreamer;
    private final MessageConverterContextFactory<U> converterContextFactory;
    private final AsyncDownloadFileHandler fileHandler;

    protected SolrIdResultStreamerFacade(
            RDFResultStreamer<T, R> rdfResultStreamer,
            ListResultStreamer<T, R> listResultStreamer,
            SolrIdBatchResultStreamer<T, R, U> solrIdBatchResultStreamer,
            MessageConverterContextFactory<U> converterContextFactory,
            AsyncDownloadFileHandler fileHandler) {
        this.rdfResultStreamer = rdfResultStreamer;
        this.listResultStreamer = listResultStreamer;
        this.solrIdBatchResultStreamer = solrIdBatchResultStreamer;
        this.converterContextFactory = converterContextFactory;
        this.fileHandler = fileHandler;
    }

    public MessageConverterContext<U> getConvertedResult(T request) {
        MediaType contentType = valueOf(request.getFormat());
        MessageConverterContext<U> context =
                converterContextFactory.get(getResource(), contentType);
        context.setFields(request.getFields());
        context.setContentType(contentType);
        Stream<String> ids = getIds(request);

        if (SUPPORTED_RDF_TYPES.containsKey(contentType)) {
            context.setEntityIds(rdfResultStreamer.stream(request, ids));
        } else if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(listResultStreamer.stream(request, ids));
        } else {
            context.setEntities(solrIdBatchResultStreamer.stream(request, ids));
        }

        return context;
    }

    private Stream<String> getIds(T request) {
        try {
            return Files.lines(fileHandler.getIdFile(request.getId()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract Resource getResource();
}
