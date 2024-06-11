package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniRefIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefIdMappingResultStreamerFacade
        extends IdMappingResultStreamerFacade<UniRefEntryLight, UniRefEntryPair> {

    protected UniRefIdMappingResultStreamerFacade(
            UniRefIdMappingRDFResultStreamer idMappingRdfStreamer,
            IdMappingListResultStreamer listResultStreamer,
            IdMappingBatchResultStreamer<UniRefEntryLight, UniRefEntryPair>
                    solrIdBatchResultStreamer,
            MessageConverterContextFactory<UniRefEntryPair> converterContextFactory,
            IdMappingJobCacheService idMappingJobCacheService) {
        super(
                idMappingRdfStreamer,
                listResultStreamer,
                solrIdBatchResultStreamer,
                converterContextFactory,
                idMappingJobCacheService);
    }

    @Override
    protected Resource getResource() {
        return Resource.UNIREF;
    }
}
