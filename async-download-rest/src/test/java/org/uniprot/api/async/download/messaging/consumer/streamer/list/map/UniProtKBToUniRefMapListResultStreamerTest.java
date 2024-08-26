package org.uniprot.api.async.download.messaging.consumer.streamer.list.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniRefMapListResultStreamerTest extends MapListResultStreamerTest<UniProtKBMapDownloadRequest> {
    @Mock
    private UniProtKBMapDownloadRequest uniProtKBMapDownloadRequest;

    @BeforeEach
    void setUp() {
        init();
        request = uniProtKBMapDownloadRequest;
        listResultStreamer =
                new UniProtKBToUniRefMapListResultStreamer(mapHeartbeatProducer, mapJobService);
    }
}
