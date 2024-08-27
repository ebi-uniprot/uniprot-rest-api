package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniParcIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntryLight;

@Component
public class UniParcIdMappingResultStreamerFacade
        extends IdMappingResultStreamerFacade<UniParcEntryLight, UniParcEntryLightPair> {

    protected UniParcIdMappingResultStreamerFacade(
            UniParcIdMappingRDFResultStreamer idMappingRdfStreamer,
            IdMappingListResultStreamer listResultStreamer,
            IdMappingBatchResultStreamer<UniParcEntryLight, UniParcEntryLightPair>
                    solrIdBatchResultStreamer,
            MessageConverterContextFactory<UniParcEntryLightPair> converterContextFactory,
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
        return Resource.UNIPARC;
    }
}
