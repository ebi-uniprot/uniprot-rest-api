package org.uniprot.api.async.download.refactor.consumer.streamer.facade.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping.IdMappingRDFStreamer;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

@Component
public class UniRefIdMappingResultStreamerFacade extends IdMappingResultStreamerFacade<UniRefEntryLight, UniRefEntryPair> {

    protected UniRefIdMappingResultStreamerFacade(IdMappingRDFStreamer idMappingRdfStreamer, IdMappingListResultStreamer listResultStreamer, IdMappingBatchResultStreamer<UniRefEntryLight, UniRefEntryPair> solrIdBatchResultStreamer, MessageConverterContextFactory<UniRefEntryPair> converterContextFactory, IdMappingJobCacheService idMappingJobCacheService) {
        super(idMappingRdfStreamer, listResultStreamer, solrIdBatchResultStreamer, converterContextFactory, idMappingJobCacheService);
    }

    @Override
    protected Resource getResourceType() {
        return Resource.UNIREF;
    }
}
