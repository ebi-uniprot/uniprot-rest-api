package org.uniprot.api.mapto.common.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.uniprot.store.config.UniProtDataType.UNIREF;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.mapto.common.service.MapToJobService;
import org.uniprot.store.config.UniProtDataType;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class MapToTaskTest {
    public static final String QUERY = "query";
    public static final UniProtDataType UNIPROT_DATA_TYPE = UNIREF;
    public static final String ID = "jobId";
    public static final String CURSOR = "cursor";
    private final RetryPolicy<Object> retryPolicy = new RetryPolicy<>();
    @Mock private MapToSearchService mapToSearchService;
    @Mock private MapToJobService mapToJobService;
    @Mock private MapToJob mapToJob;
    @Mock private MapToSearchResult searchResult;
    @Mock private CursorPage page;
    private MapToTask mapToTask;

    @BeforeEach
    void setUp() {
        lenient().when(mapToJob.getId()).thenReturn(ID);
        lenient().when(mapToJob.getQuery()).thenReturn(QUERY);
        lenient().when(mapToJob.getTargetDB()).thenReturn(UNIPROT_DATA_TYPE);
        mapToTask = new MapToTask(mapToSearchService, mapToJobService, mapToJob, retryPolicy);
    }

    @Test
    void run_beyondTheLimits() {
        when(mapToSearchService.getTargetIds(mapToJob, null)).thenReturn(searchResult);
        when(searchResult.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(1000000L);

        assertThrows(IllegalStateException.class, () -> mapToTask.run());
    }

    @Test
    void run_withinTheLimits() {
        when(mapToSearchService.getTargetIds(mapToJob, null)).thenReturn(searchResult);
        when(searchResult.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(10L);
        List<String> targetIds = getIds("target", 10);
        when(searchResult.getTargetIds()).thenReturn(targetIds);

        mapToTask.run();

        verify(mapToSearchService).getTargetIds(any(), any());
        verify(mapToJobService).setTargetIds(ID, targetIds);
    }

    @Test
    void run_withinTheLimitsAndSubsequentCalls() {
        when(searchResult.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(19L);
        when(page.hasNextPage()).thenReturn(true).thenReturn(false);
        when(page.getEncryptedNextCursor()).thenReturn(CURSOR);
        List<String> targetIds0 = getIds("target_0", 10);
        List<String> targetIds1 = getIds("target_1", 9);
        when(searchResult.getTargetIds()).thenReturn(targetIds0).thenReturn(targetIds1);
        when(mapToSearchService.getTargetIds(mapToJob, null)).thenReturn(searchResult);
        when(mapToSearchService.getTargetIds(mapToJob, CURSOR)).thenReturn(searchResult);

        mapToTask.run();

        verify(mapToSearchService, times(2)).getTargetIds(any(), any());
        verify(mapToJobService)
                .setTargetIds(
                        same(ID),
                        argThat(
                                ids ->
                                        ids.size() == 19
                                                && ids.containsAll(targetIds0)
                                                && ids.containsAll(targetIds1)));
    }

    @Test
    void run_withinTheLimitsAndSubsequentCallsOnRetry() {
        when(searchResult.getPage()).thenReturn(page);
        when(page.getTotalElements()).thenReturn(19L);
        when(page.hasNextPage()).thenReturn(true).thenReturn(false);
        when(page.getEncryptedNextCursor()).thenReturn(CURSOR);
        List<String> targetIds0 = getIds("target_0", 10);
        List<String> targetIds1 = getIds("target_1", 9);
        when(searchResult.getTargetIds()).thenReturn(targetIds0).thenReturn(targetIds1);
        when(mapToSearchService.getTargetIds(mapToJob, null)).thenReturn(searchResult);
        when(mapToSearchService.getTargetIds(mapToJob, CURSOR))
                .thenThrow(RuntimeException.class)
                .thenReturn(searchResult);

        mapToTask.run();

        verify(mapToSearchService, times(3)).getTargetIds(any(), any());
        verify(mapToJobService)
                .setTargetIds(
                        same(ID),
                        argThat(
                                ids ->
                                        ids.size() == 19
                                                && ids.containsAll(targetIds0)
                                                && ids.containsAll(targetIds1)));
    }

    private List<String> getIds(String prefix, int count) {
        List<String> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            ids.add(prefix + i);
        }

        return ids;
    }
}
