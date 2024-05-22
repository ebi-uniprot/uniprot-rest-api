package org.uniprot.api.async.download.refactor.consumer.streamer.facade.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.IdMappingResultStreamerFacadeTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping.IdMappingRDFStreamer;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniRefIdMappingResultStreamerFacadeTest extends IdMappingResultStreamerFacadeTest<UniRefEntryLight, UniRefEntryPair> {
    @Mock
    private IdMappingRDFStreamer rdfResultStreamer;
    @Mock
    private IdMappingListResultStreamer listResultStreamer;
    @Mock
    private IdMappingBatchResultStreamer<UniRefEntryLight, UniRefEntryPair> uniRefIdMappingBatchResultStreamer;
    @Mock
    private MessageConverterContextFactory<UniRefEntryPair> uniRefConverterContextFactory;
    @Mock
    private IdMappingJobCacheService idMappingJobCacheService;

    @BeforeEach
    void setUp() {
        super.rdfResultStreamer = rdfResultStreamer;
        super.listResultStreamer = listResultStreamer;
        super.idMappingJobCacheService = idMappingJobCacheService;
        converterContextFactory = uniRefConverterContextFactory;
        idMappingBatchResultStreamer = uniRefIdMappingBatchResultStreamer;
        idMappingResultStreamerFacade = new UniRefIdMappingResultStreamerFacade(rdfResultStreamer, listResultStreamer, uniRefIdMappingBatchResultStreamer, uniRefConverterContextFactory, idMappingJobCacheService);
        mock();
    }


}
