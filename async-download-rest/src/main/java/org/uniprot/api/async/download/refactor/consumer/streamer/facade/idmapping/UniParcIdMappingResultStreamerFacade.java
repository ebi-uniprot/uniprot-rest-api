package org.uniprot.api.async.download.refactor.consumer.streamer.facade.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping.IdMappingRDFStreamer;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntry;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.*;

@Component
public class UniParcIdMappingResultStreamerFacade  extends IdMappingResultStreamerFacade<UniParcEntry, UniParcEntryPair> {

    protected UniParcIdMappingResultStreamerFacade(IdMappingRDFStreamer idMappingRdfStreamer, IdMappingListResultStreamer listResultStreamer, IdMappingBatchResultStreamer<UniParcEntry, UniParcEntryPair> solrIdBatchResultStreamer, MessageConverterContextFactory<UniParcEntryPair> converterContextFactory, IdMappingJobCacheService idMappingJobCacheService) {
        super(idMappingRdfStreamer, listResultStreamer, solrIdBatchResultStreamer, converterContextFactory, idMappingJobCacheService);
    }

    @Override
    protected Resource getResource() {
        return Resource.UNIPARC;
    }
}
