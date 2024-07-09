package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniProtKBRDFResultStreamer
        extends RDFResultStreamer<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private static final String UNIPROTKB_DATA_TYPE = "uniprotkb";

    public UniProtKBRDFResultStreamer(
            UniProtKBHeartbeatProducer heartbeatProducer,
            UniProtKBJobService jobService,
            RdfStreamer uniProtRdfStreamer) {
        super(heartbeatProducer, jobService, uniProtRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return UNIPROTKB_DATA_TYPE;
    }
}
