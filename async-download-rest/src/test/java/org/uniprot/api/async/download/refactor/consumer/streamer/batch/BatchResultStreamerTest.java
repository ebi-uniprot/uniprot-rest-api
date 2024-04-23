package org.uniprot.api.async.download.refactor.consumer.streamer.batch;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

public abstract class BatchResultStreamerTest<T extends DownloadRequest, R extends DownloadJob, S> {
    private static final String JOB_ID = "jobId";
    protected T request;
    protected R job;
    protected HeartbeatProducer heartbeatProducer;
    protected JobService<R> jobService;
    protected BatchResultStreamer<T, R, S> batchResultStreamer;

    @Test
    void stream() {
        mockBatch();
        when(request.getJobId()).thenReturn(JOB_ID);
        when(jobService.find(JOB_ID)).thenReturn(Optional.ofNullable(job));
        List<String> idList = List.of("id1", "id2", "id3");
        Stream<String> ids = idList.stream();

        Stream<S> result = batchResultStreamer.stream(request, ids);

        assertThat(result).hasSameElementsAs(getEntryList());
        InOrder inOrder = inOrder(heartbeatProducer);
        inOrder.verify(heartbeatProducer).createForResults(job, 2);
        inOrder.verify(heartbeatProducer).createForResults(job, 1);
    }

    @Test
    void stream_incorrectJobId() {
        when(request.getJobId()).thenReturn(JOB_ID);
        when(jobService.find(JOB_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> batchResultStreamer.stream(request, Stream.of()));

    }

    protected abstract void mockBatch();

    protected abstract Iterable<S> getEntryList();
}