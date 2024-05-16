package org.uniprot.api.async.download.refactor.consumer.streamer.facade;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping.IdMappingRDFStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingServiceUtils;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

public abstract class IdMappingResultStreamerFacade<Q, P extends EntryPair<Q>> {
    private static final Map<MediaType, String> SUPPORTED_RDF_TYPES =
            Map.of(
                    RDF_MEDIA_TYPE, "rdf",
                    TURTLE_MEDIA_TYPE, "ttl",
                    N_TRIPLES_MEDIA_TYPE, "nt");
    // todo resuse the existing or create common
    private final IdMappingRDFStreamer rdfResultStreamer;
    private final IdMappingListResultStreamer listResultStreamer;
    private final IdMappingBatchResultStreamer<Q, P> solrIdBatchResultStreamer;
    private final MessageConverterContextFactory<P> converterContextFactory;
    private final IdMappingJobCacheService idMappingJobCacheService;

    protected IdMappingResultStreamerFacade(
            IdMappingRDFStreamer rdfResultStreamer,
            IdMappingListResultStreamer listResultStreamer,
            IdMappingBatchResultStreamer<Q, P> solrIdBatchResultStreamer,
            MessageConverterContextFactory<P> converterContextFactory,
            IdMappingJobCacheService idMappingJobCacheService) {
        this.rdfResultStreamer = rdfResultStreamer;
        this.listResultStreamer = listResultStreamer;
        this.solrIdBatchResultStreamer = solrIdBatchResultStreamer;
        this.converterContextFactory = converterContextFactory;
        this.idMappingJobCacheService = idMappingJobCacheService;
    }

    public MessageConverterContext<P> getConvertedResult(IdMappingDownloadRequest request) {
        IdMappingJob idMappingJobInput =
                Optional.ofNullable(
                                idMappingJobCacheService.getCompletedJobAsResource(
                                        request.getJobId()))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid Job Id " + request.getJobId()));
        IdMappingResult idMappingResult = idMappingJobInput.getIdMappingResult();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        MessageConverterContext<P> context =
                converterContextFactory.get(getResourceType(), contentType);
        ExtraOptions extraOptions = IdMappingServiceUtils.getExtraOptions(idMappingResult);
        context.setExtraOptions(extraOptions);
        context.setWarnings(idMappingResult.getWarnings());
        context.setFields(request.getFields());
        context.setContentType(contentType);

        if (SUPPORTED_RDF_TYPES.containsKey(contentType)) {
            context.setEntityIds(
                    rdfResultStreamer.stream(request, getToIds(idMappingResult).stream()));
        } else if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(
                    listResultStreamer.stream(request, getToIds(idMappingResult).stream()));
        } else {
            context.setEntities(
                    solrIdBatchResultStreamer.stream(
                            request, idMappingResult.getMappedIds().stream()));
        }

        return context;
    }

    protected abstract Resource getResourceType();

    private Set<String> getToIds(IdMappingResult idMappingResult) {
        return idMappingResult.getMappedIds().stream()
                .map(IdMappingStringPair::getTo)
                .collect(Collectors.toSet());
    }
}
