package org.uniprot.api.async.download.refactor.consumer.streamer.batch;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

public abstract class IdMappingBatchResultStreamerTest<Q, P extends EntryPair<Q>> {
    private static final String JOB_ID = "jobId";
    protected IdMappingJobService jobService;
    protected IdMappingHeartbeatProducer heartbeatProducer;
    @Mock
    private IdMappingDownloadJob job;
    @Mock
    private IdMappingDownloadRequest request;
    protected IdMappingBatchResultStreamer<Q, P> idMappingBatchResultStreamer;

    @Test
    void stream() {
        mockBatch();
        when(request.getJobId()).thenReturn(JOB_ID);
        when(jobService.find(JOB_ID)).thenReturn(Optional.ofNullable(job));
        List<IdMappingStringPair> idMappingStringPairList = List.of(new IdMappingStringPair("from1", "to1"), new IdMappingStringPair("from2", "to2"), new IdMappingStringPair("from3", "to3"));
        Stream<IdMappingStringPair> idMappingPairs = idMappingStringPairList.stream();

        List<P> result = idMappingBatchResultStreamer.stream(request, idMappingPairs).collect(Collectors.toList());

        assertThat(result).hasSameElementsAs(getEntryList());
        InOrder inOrder = inOrder(heartbeatProducer);
        inOrder.verify(heartbeatProducer).createForResults(job, 2);
        inOrder.verify(heartbeatProducer).createForResults(job, 1);
    }

    protected abstract Iterable<P> getEntryList();

    protected abstract void mockBatch();

    @Test
    void stream_incorrectJobId() {
        when(request.getJobId()).thenReturn(JOB_ID);
        when(jobService.find(JOB_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> idMappingBatchResultStreamer.stream(request, Stream.of()));
    }
}