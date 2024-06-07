package org.uniprot.api.async.download.messaging.consumer.streamer.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;

public abstract class ListResultStreamerTest<T extends DownloadRequest, R extends DownloadJob> {
    private static final String ID = "someId";
    protected T request;
    protected R job;
    protected HeartbeatProducer heartbeatProducer;
    protected JobService<R> jobService;
    protected ListResultStreamer<T, R> listResultStreamer;

    @Test
    void stream() {
        when(request.getId()).thenReturn(ID);
        when(jobService.find(ID)).thenReturn(Optional.ofNullable(job));
        List<String> idList = List.of("id1", "id2", "id3");
        Stream<String> ids = idList.stream();

        Stream<String> result = listResultStreamer.stream(request, ids);

        assertThat(result).hasSameElementsAs(idList);
        verify(heartbeatProducer, times(3)).createForResults(job, 1L);
    }

    @Test
    void stream_incorrectJobId() {
        when(request.getId()).thenReturn(ID);
        when(jobService.find(ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> listResultStreamer.stream(request, Stream.of()));
    }
}
