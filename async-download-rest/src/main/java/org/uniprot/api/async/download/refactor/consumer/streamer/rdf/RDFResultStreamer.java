package org.uniprot.api.async.download.refactor.consumer.streamer.rdf;

import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Map;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.IdResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.output.UniProtMediaType;

public abstract class RDFResultStreamer<T extends DownloadRequest, R extends DownloadJob>
        extends IdResultStreamer<T, R, String> {

    private static final Map<MediaType, String> SUPPORTED_RDF_TYPES =
            Map.of(
                    RDF_MEDIA_TYPE, "rdf",
                    TURTLE_MEDIA_TYPE, "ttl",
                    N_TRIPLES_MEDIA_TYPE, "nt");
    // todo using common, look at T,R, S naming convention
    private final HeartbeatProducer heartbeatProducer;
    private final RdfStreamer rdfStreamer;

    protected RDFResultStreamer(
            HeartbeatProducer heartbeatProducer,
            JobService<R> jobService,
            RdfStreamer rdfStreamer) {
        super(jobService);
        this.heartbeatProducer = heartbeatProducer;
        this.rdfStreamer = rdfStreamer;
    }

    @Override
    public Stream<String> stream(T request, Stream<String> ids) {
        R job = getJob(request);
        return rdfStreamer.stream(
                ids,
                getDataType(),
                SUPPORTED_RDF_TYPES.get(UniProtMediaType.valueOf(request.getFormat())),
                entries -> heartbeatProducer.createForResults(job, entries));
    }

    protected abstract String getDataType();
}
