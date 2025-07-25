package org.uniprot.api.mapto.common.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;

public class PageableUtils {

    public static Pageable getPageable(CursorPage cursorPage) {
        long nextPageOffset = cursorPage.getOffset() + cursorPage.getPageSize();
        int pageSize = (int) (nextPageOffset - cursorPage.getOffset());
        int pageNumber =
                calculatePageNumber(
                        cursorPage.getOffset(), cursorPage.getTotalElements(), pageSize);
        return PageRequest.of(pageNumber, pageSize);
    }

    private static int calculatePageNumber(long offset, long totalElements, int pageSize) {
        if (offset >= totalElements) {
            throw new IllegalArgumentException("Offset exceeds total number of elements");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        return (int) (offset / pageSize);
    }
}
