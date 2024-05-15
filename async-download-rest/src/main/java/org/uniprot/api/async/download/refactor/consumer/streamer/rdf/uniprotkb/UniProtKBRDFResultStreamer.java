package org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniProtKBRDFResultStreamer
        extends RDFResultStreamer<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private static final String UNI_PROT_KB_DATA_TYPE = "uniprotkb";

    public UniProtKBRDFResultStreamer(
            UniProtKBHeartbeatProducer heartbeatProducer,
            UniProtKBJobService jobService,
            RdfStreamer uniProtRdfStreamer) {
        super(heartbeatProducer, jobService, uniProtRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return UNI_PROT_KB_DATA_TYPE;
    }
}
