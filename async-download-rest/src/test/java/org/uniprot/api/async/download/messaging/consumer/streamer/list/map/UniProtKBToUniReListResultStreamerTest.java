package org.uniprot.api.async.download.messaging.consumer.streamer.list.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniReListResultStreamerTest
        extends MapListResultStreamerTest<UniProtKBToUniRefDownloadRequest> {
    @Mock private UniProtKBToUniRefDownloadRequest uniProtKBToUniRefMapDownloadRequest;

    @BeforeEach
    void setUp() {
        init();
        request = uniProtKBToUniRefMapDownloadRequest;
        listResultStreamer =
                new UniProtKBToUniReToListResultStreamer(mapToHeartbeatProducer, mapToJobService);
    }
}
