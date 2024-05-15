package org.uniprot.api.async.download.refactor.consumer.streamer.facade;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.BatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import java.util.Map;
import java.util.stream.Stream;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.*;

public abstract class ResultStreamerFacade<T extends DownloadRequest, R extends DownloadJob, S>{
    private static final Map<MediaType, String> SUPPORTED_RDF_TYPES =
            Map.of(
                    RDF_MEDIA_TYPE, "rdf",
                    TURTLE_MEDIA_TYPE, "ttl",
                    N_TRIPLES_MEDIA_TYPE, "nt");
    //todo resuse the existing or create common
    private final RDFResultStreamer<T, R> rdfResultStreamer;
    private final ListResultStreamer<T, R> listResultStreamer;
    private final BatchResultStreamer<T, R, S> batchResultStreamer;
    private final MessageConverterContextFactory<S> converterContextFactory;

    protected ResultStreamerFacade(RDFResultStreamer<T, R> rdfResultStreamer, ListResultStreamer<T, R> listResultStreamer, BatchResultStreamer<T, R, S> batchResultStreamer, MessageConverterContextFactory<S> converterContextFactory) {
        this.rdfResultStreamer = rdfResultStreamer;
        this.listResultStreamer = listResultStreamer;
        this.batchResultStreamer = batchResultStreamer;
        this.converterContextFactory = converterContextFactory;
    }

    public MessageConverterContext<S> getConvertedResult(T request, Stream<String> ids) {
        MediaType contentType = valueOf(request.getFormat());
        MessageConverterContext<S> context = converterContextFactory.get(getResource(), contentType);
        context.setFields(request.getFields());
        context.setContentType(contentType);

        if (SUPPORTED_RDF_TYPES.containsKey(contentType)) {
            context.setEntityIds(rdfResultStreamer.stream(request, ids));
        } else if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(listResultStreamer.stream(request, ids));
        } else {
            context.setEntities(batchResultStreamer.stream(request, ids));
        }

        return context;
    }

    protected abstract Resource getResource();

}
