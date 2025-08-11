package org.uniprot.api.mapto.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;

@ExtendWith(MockitoExtension.class)
class PageableUtilsTest {
    @Mock private CursorPage cp;

    // ---------- successful paths ----------

    @Test
    void testGetPageable_firstPage() {
        long offset = 0;
        int pageSize = 10;
        long total = 100;

        when(cp.getOffset()).thenReturn(offset);
        when(cp.getTotalElements()).thenReturn(total);
        when(cp.getPageSize()).thenReturn(pageSize);

        Pageable pageable = PageableUtils.getPageable(cp);
        assertEquals(PageRequest.of(0, pageSize), pageable);
    }

    @Test
    void testGetPageable_middlePage() {
        long offset = 20;
        int pageSize = 10;
        long total = 100;

        when(cp.getOffset()).thenReturn(offset);
        when(cp.getTotalElements()).thenReturn(total);
        when(cp.getPageSize()).thenReturn(pageSize);

        Pageable pageable = PageableUtils.getPageable(cp);

        assertEquals(PageRequest.of(2, pageSize), pageable);
    }

    @Test
    void testGetPageable_irregular() {
        long offset = 20;
        int pageSize = 10;
        long total = 27;

        when(cp.getOffset()).thenReturn(offset);
        when(cp.getTotalElements()).thenReturn(total);
        when(cp.getPageSize()).thenReturn(pageSize);

        Pageable pageable = PageableUtils.getPageable(cp);

        assertEquals(PageRequest.of(2, 10), pageable);
    }

    @Test
    void testGetPageableThrowsIfOffsetTooLarge() {
        when(cp.getOffset()).thenReturn(1000L);
        when(cp.getPageSize()).thenReturn(10);
        when(cp.getTotalElements()).thenReturn(10L);

        Exception exception =
                assertThrows(IllegalArgumentException.class, () -> PageableUtils.getPageable(cp));

        assertTrue(exception.getMessage().contains("Offset exceeds total number of elements"));
    }

    @Test
    void testGetPageableThrowsIfPageSizeZero() {
        when(cp.getPageSize()).thenReturn(0);
        when(cp.getTotalElements()).thenReturn(10L);

        Exception exception =
                assertThrows(IllegalArgumentException.class, () -> PageableUtils.getPageable(cp));

        assertTrue(exception.getMessage().contains("Page size must be greater than 0"));
    }
}
