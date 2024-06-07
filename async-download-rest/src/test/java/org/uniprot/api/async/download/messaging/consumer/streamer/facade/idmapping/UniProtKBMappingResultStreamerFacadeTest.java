package org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.IdMappingResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping.UniProtKBIdMappingRDFResultStreamer;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
public class UniProtKBMappingResultStreamerFacadeTest extends IdMappingResultStreamerFacadeTest<UniProtKBEntry, UniProtKBEntryPair> {
    @Mock
    private UniProtKBIdMappingRDFResultStreamer rdfResultStreamer;
    @Mock
    private IdMappingListResultStreamer listResultStreamer;
    @Mock
    private IdMappingBatchResultStreamer<UniProtKBEntry, UniProtKBEntryPair> uniProtKBIdMappingBatchResultStreamer;
    @Mock
    private MessageConverterContextFactory<UniProtKBEntryPair> uniProtKBConverterContextFactory;
    @Mock
    private IdMappingJobCacheService idMappingJobCacheService;

    @BeforeEach
    void setUp() {
        super.rdfResultStreamer = rdfResultStreamer;
        super.listResultStreamer = listResultStreamer;
        super.idMappingJobCacheService = idMappingJobCacheService;
        converterContextFactory = uniProtKBConverterContextFactory;
        idMappingBatchResultStreamer = uniProtKBIdMappingBatchResultStreamer;
        idMappingResultStreamerFacade = new UniProtKBIdMappingResultStreamerFacade(rdfResultStreamer, listResultStreamer, uniProtKBIdMappingBatchResultStreamer, uniProtKBConverterContextFactory, idMappingJobCacheService);
        mock();
    }


}
