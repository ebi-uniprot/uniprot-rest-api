package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniParcIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniParcLightIdMappingResultStreamerFacadeTest
        extends IdMappingResultStreamerFacadeTest<UniParcEntryLight, UniParcEntryLightPair> {
    @Mock private UniParcIdMappingRDFResultStreamer rdfResultStreamer;
    @Mock private IdMappingListResultStreamer listResultStreamer;

    @Mock
    private IdMappingBatchResultStreamer<UniParcEntryLight, UniParcEntryLightPair>
            uniParcIdMappingBatchResultStreamer;

    @Mock
    private MessageConverterContextFactory<UniParcEntryLightPair> uniParcConverterContextFactory;

    @Mock private IdMappingJobCacheService idMappingJobCacheService;

    @BeforeEach
    void setUp() {
        super.rdfResultStreamer = rdfResultStreamer;
        super.listResultStreamer = listResultStreamer;
        super.idMappingJobCacheService = idMappingJobCacheService;
        converterContextFactory = uniParcConverterContextFactory;
        idMappingBatchResultStreamer = uniParcIdMappingBatchResultStreamer;
        idMappingResultStreamerFacade =
                new UniParcLightIdMappingResultStreamerFacade(
                        rdfResultStreamer,
                        listResultStreamer,
                        uniParcIdMappingBatchResultStreamer,
                        uniParcConverterContextFactory,
                        idMappingJobCacheService);
        mock();
    }
}
