package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniParcIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntry;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

@Component
public class UniParcIdMappingResultStreamerFacade  extends IdMappingResultStreamerFacade<UniParcEntry, UniParcEntryPair> {

    protected UniParcIdMappingResultStreamerFacade(UniParcIdMappingRDFResultStreamer idMappingRdfStreamer, IdMappingListResultStreamer listResultStreamer, IdMappingBatchResultStreamer<UniParcEntry, UniParcEntryPair> solrIdBatchResultStreamer, MessageConverterContextFactory<UniParcEntryPair> converterContextFactory, IdMappingJobCacheService idMappingJobCacheService) {
        super(idMappingRdfStreamer, listResultStreamer, solrIdBatchResultStreamer, converterContextFactory, idMappingJobCacheService);
    }

    @Override
    protected Resource getResource() {
        return Resource.UNIPARC;
    }
}
