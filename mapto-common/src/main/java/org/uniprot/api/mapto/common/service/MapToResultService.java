package org.uniprot.api.mapto.common.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.repository.MapToResultRepository;

@Service
public class MapToResultService {
    private final MapToResultRepository mapToResultRepository;

    public MapToResultService(MapToResultRepository mapToResultRepository) {
        this.mapToResultRepository = mapToResultRepository;
    }

    public List<String> findAllTargetIdsByMapToJob(MapToJob mapToJob) {
        return mapToResultRepository.findTargetIdByMapToJob(mapToJob);
    }

    public List<String> findTargetIdsByMapToJob(MapToJob mapToJob, CursorPage cursorPage) {
        Pageable pageable = getPageable(mapToJob, cursorPage);
        return mapToResultRepository.findTargetIdByMapToJob(mapToJob, pageable);
    }

    private static Pageable getPageable(MapToJob mapToJob, CursorPage cursorPage) {
        int pageSize = (int) (CursorPage.getNextOffset(cursorPage) - cursorPage.getOffset());
        pageSize = pageSize > 0 ? pageSize : cursorPage.getPageSize();
        int pageNumber =
                calculatePageNumber(cursorPage.getOffset(), mapToJob.getTotalTargetIds(), pageSize);
        return PageRequest.of(pageNumber, pageSize);
    }

    private static int calculatePageNumber(long offset, long totalElements, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (offset >= totalElements) {
            throw new IllegalArgumentException("Offset exceeds total number of elements");
        }
        return (int) (offset / pageSize);
    }
}
