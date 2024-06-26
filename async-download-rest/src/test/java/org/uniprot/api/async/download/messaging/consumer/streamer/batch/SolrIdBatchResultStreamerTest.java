package org.uniprot.api.async.download.messaging.consumer.streamer.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class SolrIdBatchResultStreamerTest<
        T extends DownloadRequest, R extends DownloadJob, S> {
    private static final String ID = "someId";
    protected T request;
    protected R job;
    protected HeartbeatProducer heartbeatProducer;
    protected JobService<R> jobService;
    protected SolrIdBatchResultStreamer<T, R, S> solrIdBatchResultStreamer;

    @Test
    void stream() {
        mockBatch();
        when(request.getDownloadJobId()).thenReturn(ID);
        when(jobService.find(ID)).thenReturn(Optional.ofNullable(job));
        List<String> idList = List.of("id1", "id2", "id3");
        Stream<String> ids = idList.stream();

        Stream<S> result = solrIdBatchResultStreamer.stream(request, ids);

        assertThat(result).hasSameElementsAs(getEntryList());
        InOrder inOrder = inOrder(heartbeatProducer);
        inOrder.verify(heartbeatProducer).generateForResults(job, 2);
        inOrder.verify(heartbeatProducer).generateForResults(job, 1);
    }

    @Test
    void stream_incorrectJobId() {
        when(request.getDownloadJobId()).thenReturn(ID);
        when(jobService.find(ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> solrIdBatchResultStreamer.stream(request, Stream.of()));
    }

    protected abstract void mockBatch();

    protected abstract Iterable<S> getEntryList();
}
