package org.uniprot.api.mapto.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.repository.MapToResultRepository;

@ExtendWith(MockitoExtension.class)
class MapToResultServiceTest {

    private static final long TOTAL_TARGET_IDS = 200L;
    private static final List<String> TARGET_IDS = List.of("target0", "target1");
    @Mock private MapToResultRepository mapToResultRepository;

    @Mock private CursorPage cursorPage;

    @Mock private MapToJob mapToJob;

    @InjectMocks private MapToResultService mapToResultService;

    @BeforeEach
    void setUp() {
        lenient().when(mapToJob.getTotalTargetIds()).thenReturn(TOTAL_TARGET_IDS);
    }

    @Test
    void testFindAllTargetIdsByMapToJob() {

        when(mapToResultRepository.findTargetIdByMapToJob(mapToJob)).thenReturn(TARGET_IDS);

        List<String> result = mapToResultRepository.findTargetIdByMapToJob(mapToJob);

        assertEquals(2, result.size());
        assertEquals("target0", result.get(0));
        verify(mapToResultRepository, times(1)).findTargetIdByMapToJob(mapToJob);
    }

    @Test
    void testFindTargetIdsByMapToJobWithPaging() {
        when(cursorPage.getPageSize()).thenReturn(10);
        when(cursorPage.getOffset()).thenReturn(20L);
        when(cursorPage.getTotalElements()).thenReturn(200L);

        when(mapToResultRepository.findTargetIdByMapToJob(eq(mapToJob), any(Pageable.class)))
                .thenReturn(TARGET_IDS);

        List<String> result = mapToResultService.findTargetIdsByMapToJob(mapToJob, cursorPage);

        assertEquals(2, result.size());
        assertEquals("target0", result.get(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(mapToResultRepository)
                .findTargetIdByMapToJob(eq(mapToJob), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber()); // 20 / 10 = page 2
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void testGetPageableThrowsIfOffsetTooLarge() {
        when(cursorPage.getOffset()).thenReturn(1000L);
        when(cursorPage.getPageSize()).thenReturn(10);
        when(cursorPage.getTotalElements()).thenReturn(10L);

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mapToResultService.findTargetIdsByMapToJob(mapToJob, cursorPage));

        assertTrue(exception.getMessage().contains("Offset exceeds total number of elements"));
    }

    @Test
    void testGetPageableThrowsIfPageSizeZero() {
        when(cursorPage.getPageSize()).thenReturn(0);

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mapToResultService.findTargetIdsByMapToJob(mapToJob, cursorPage));

        assertTrue(exception.getMessage().contains("Page size must be greater than 0"));
    }
}
