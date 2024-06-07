package org.uniprot.api.async.download.messaging.consumer.streamer.rdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

public abstract class RDFResultStreamerTest<T extends DownloadRequest, R extends DownloadJob> {
    private static final String ID = "someId";
    private static final String APPLICATION_RDF_XML = "application/rdf+xml";
    public static final String RDF = "rdf";
    protected T request;
    protected R job;
    protected HeartbeatProducer heartbeatProducer;
    protected JobService<R> jobService;
    protected RDFResultStreamer<T, R> rdfResultStreamer;
    @Mock protected RdfStreamer rdfStreamer;

    @Test
    void stream() {
        when(request.getId()).thenReturn(ID);
        when(jobService.find(ID)).thenReturn(Optional.ofNullable(job));
        when(request.getFormat()).thenReturn(APPLICATION_RDF_XML);
        Stream<String> ids = Stream.of("id1", "id2", "id3");
        List<String> redfList = List.of("rdf1", "rdf2", "rdf3");
        Stream<String> rdf = redfList.stream();
        when(rdfStreamer.stream(
                        eq(ids),
                        eq(rdfResultStreamer.getDataType()),
                        eq(RDF),
                        any(LongConsumer.class)))
                .thenReturn(rdf);

        Stream<String> result = rdfResultStreamer.stream(request, ids);

        assertThat(result).hasSameElementsAs(redfList);
    }

    @Test
    void stream_incorrectJobId() {
        when(request.getId()).thenReturn(ID);
        when(jobService.find(ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> rdfResultStreamer.stream(request, Stream.of()));
    }
}
