package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniparc;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniParcRDFResultStreamer
        extends RDFResultStreamer<UniParcDownloadRequest, UniParcDownloadJob> {
    private static final String UNIPARC_DATA_TYPE = "uniparc";

    public UniParcRDFResultStreamer(
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcJobService jobService,
            RdfStreamer uniParcRdfStreamer) {
        super(heartbeatProducer, jobService, uniParcRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return UNIPARC_DATA_TYPE;
    }
}
