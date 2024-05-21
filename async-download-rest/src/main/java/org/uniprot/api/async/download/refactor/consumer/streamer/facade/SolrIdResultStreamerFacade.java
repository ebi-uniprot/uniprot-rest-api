package org.uniprot.api.async.download.refactor.consumer.streamer.facade;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

public abstract class SolrIdResultStreamerFacade<T extends DownloadRequest, R extends DownloadJob, S> {
    private static final Map<MediaType, String> SUPPORTED_RDF_TYPES =
            Map.of(
                    RDF_MEDIA_TYPE, "rdf",
                    TURTLE_MEDIA_TYPE, "ttl",
                    N_TRIPLES_MEDIA_TYPE, "nt");
    // todo resuse the existing or create common
    private final RDFResultStreamer<T, R> rdfResultStreamer;
    private final ListResultStreamer<T, R> listResultStreamer;
    private final SolrIdBatchResultStreamer<T, R, S> solrIdBatchResultStreamer;
    private final MessageConverterContextFactory<S> converterContextFactory;
    private final AsyncDownloadFileHandler fileHandler;

    protected SolrIdResultStreamerFacade(
            RDFResultStreamer<T, R> rdfResultStreamer,
            ListResultStreamer<T, R> listResultStreamer,
            SolrIdBatchResultStreamer<T, R, S> solrIdBatchResultStreamer,
            MessageConverterContextFactory<S> converterContextFactory, AsyncDownloadFileHandler fileHandler) {
        this.rdfResultStreamer = rdfResultStreamer;
        this.listResultStreamer = listResultStreamer;
        this.solrIdBatchResultStreamer = solrIdBatchResultStreamer;
        this.converterContextFactory = converterContextFactory;
        this.fileHandler = fileHandler;
    }

    public MessageConverterContext<S> getConvertedResult(T request) {
        MediaType contentType = valueOf(request.getFormat());
        MessageConverterContext<S> context =
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
            return Files.lines(fileHandler.getIdFile(request.getJobId()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract Resource getResource();
}
