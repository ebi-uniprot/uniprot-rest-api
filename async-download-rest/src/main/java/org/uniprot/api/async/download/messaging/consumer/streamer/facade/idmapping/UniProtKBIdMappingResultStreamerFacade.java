package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniProtKBIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

@Component
public class UniProtKBIdMappingResultStreamerFacade extends IdMappingResultStreamerFacade<UniProtKBEntry, UniProtKBEntryPair> {

    protected UniProtKBIdMappingResultStreamerFacade(UniProtKBIdMappingRDFResultStreamer idMappingRdfStreamer, IdMappingListResultStreamer listResultStreamer, IdMappingBatchResultStreamer<UniProtKBEntry, UniProtKBEntryPair> solrIdBatchResultStreamer, MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory, IdMappingJobCacheService idMappingJobCacheService) {
        super(idMappingRdfStreamer, listResultStreamer, solrIdBatchResultStreamer, converterContextFactory, idMappingJobCacheService);
    }

    @Override
    protected Resource getResource() {
        return Resource.UNIPROTKB;
    }
}
