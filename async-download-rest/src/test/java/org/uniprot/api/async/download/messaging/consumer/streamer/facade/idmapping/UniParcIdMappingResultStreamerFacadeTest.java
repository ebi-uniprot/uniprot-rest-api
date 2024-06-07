package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniParcIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntry;

@ExtendWith(MockitoExtension.class)
public class UniParcIdMappingResultStreamerFacadeTest extends IdMappingResultStreamerFacadeTest<UniParcEntry, UniParcEntryPair> {
    @Mock
    private UniParcIdMappingRDFResultStreamer rdfResultStreamer;
    @Mock
    private IdMappingListResultStreamer listResultStreamer;
    @Mock
    private IdMappingBatchResultStreamer<UniParcEntry, UniParcEntryPair> uniParcIdMappingBatchResultStreamer;
    @Mock
    private MessageConverterContextFactory<UniParcEntryPair> uniParcConverterContextFactory;
    @Mock
    private IdMappingJobCacheService idMappingJobCacheService;

    @BeforeEach
    void setUp() {
        super.rdfResultStreamer = rdfResultStreamer;
        super.listResultStreamer = listResultStreamer;
        super.idMappingJobCacheService = idMappingJobCacheService;
        converterContextFactory = uniParcConverterContextFactory;
        idMappingBatchResultStreamer = uniParcIdMappingBatchResultStreamer;
        idMappingResultStreamerFacade = new UniParcIdMappingResultStreamerFacade(rdfResultStreamer, listResultStreamer, uniParcIdMappingBatchResultStreamer, uniParcConverterContextFactory, idMappingJobCacheService);
        mock();
    }


}
