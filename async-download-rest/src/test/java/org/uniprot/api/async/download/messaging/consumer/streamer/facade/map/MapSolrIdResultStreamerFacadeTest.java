package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;

public abstract class MapSolrIdResultStreamerFacadeTest<T extends MapToDownloadRequest, S>
        extends SolrIdResultStreamerFacadeTest<T, MapToDownloadJob, S> {
    @Mock protected MapToFileHandler mapToFileHandler;

    void init() {
        fileHandler = mapToFileHandler;
    }
}
