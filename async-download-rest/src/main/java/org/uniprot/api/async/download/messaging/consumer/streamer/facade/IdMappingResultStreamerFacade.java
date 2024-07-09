package org.uniprot.api.async.download.messaging.consumer.streamer.facade;

import static org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer.SUPPORTED_RDF_TYPES;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.IdMappingRDFStreamer;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
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

public abstract class IdMappingResultStreamerFacade<U, V extends EntryPair<U>> {
    private final IdMappingRDFStreamer rdfResultStreamer;
    private final IdMappingListResultStreamer listResultStreamer;
    private final IdMappingBatchResultStreamer<U, V> idMappingBatchResultStreamer;
    private final MessageConverterContextFactory<V> converterContextFactory;
    private final IdMappingJobCacheService idMappingJobCacheService;

    protected IdMappingResultStreamerFacade(
            IdMappingRDFStreamer rdfResultStreamer,
            IdMappingListResultStreamer listResultStreamer,
            IdMappingBatchResultStreamer<U, V> idMappingBatchResultStreamer,
            MessageConverterContextFactory<V> converterContextFactory,
            IdMappingJobCacheService idMappingJobCacheService) {
        this.rdfResultStreamer = rdfResultStreamer;
        this.listResultStreamer = listResultStreamer;
        this.idMappingBatchResultStreamer = idMappingBatchResultStreamer;
        this.converterContextFactory = converterContextFactory;
        this.idMappingJobCacheService = idMappingJobCacheService;
    }

    public MessageConverterContext<V> getConvertedResult(IdMappingDownloadRequest request) {
        IdMappingJob idMappingJobInput =
                Optional.ofNullable(
                                idMappingJobCacheService.getJobAsResource(
                                        request.getIdMappingJobId()))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid Job Id " + request.getIdMappingJobId()));
        IdMappingResult idMappingResult = idMappingJobInput.getIdMappingResult();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        MessageConverterContext<V> context =
                converterContextFactory.get(getResource(), contentType);
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
                    idMappingBatchResultStreamer.stream(
                            request, idMappingResult.getMappedIds().stream()));
        }

        return context;
    }

    protected abstract Resource getResource();

    private Set<String> getToIds(IdMappingResult idMappingResult) {
        return idMappingResult.getMappedIds().stream()
                .map(IdMappingStringPair::getTo)
                .collect(Collectors.toSet());
    }
}
