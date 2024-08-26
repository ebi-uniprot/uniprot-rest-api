package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;

public abstract class MapSolrIdResultStreamerFacadeTest<T extends MapDownloadRequest, S>
        extends SolrIdResultStreamerFacadeTest<T, MapDownloadJob, S> {
    @Mock protected MapFileHandler mapFileHandler;

    void init() {
        fileHandler = mapFileHandler;
    }
}
