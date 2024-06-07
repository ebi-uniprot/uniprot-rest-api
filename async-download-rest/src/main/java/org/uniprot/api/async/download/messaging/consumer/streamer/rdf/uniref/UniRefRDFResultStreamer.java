package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniRefRDFResultStreamer
        extends RDFResultStreamer<UniRefDownloadRequest, UniRefDownloadJob> {
    private static final String UNI_REF_DATA_TYPE = "uniref";

    public UniRefRDFResultStreamer(
            UniRefHeartbeatProducer heartbeatProducer,
            UniRefJobService jobService,
            RdfStreamer uniProtRdfStreamer) {
        super(heartbeatProducer, jobService, uniProtRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return UNI_REF_DATA_TYPE;
    }
}
