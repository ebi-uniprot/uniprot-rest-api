package org.uniprot.api.mapto.common.service;

import static org.uniprot.api.mapto.common.service.PageableUtils.getPageable;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.repository.MapToResultRepository;

@Service
public class MapToResultService {
    private final MapToResultRepository mapToResultRepository;

    public MapToResultService(MapToResultRepository mapToResultRepository) {
        this.mapToResultRepository = mapToResultRepository;
    }

    @Transactional(readOnly = true)
    public List<String> findAllTargetIdsByMapToJob(MapToJob mapToJob) {
        return streamAllTargetIdsByMapToJob(mapToJob).toList();
    }

    public Stream<String> streamAllTargetIdsByMapToJob(MapToJob mapToJob) {
        return mapToResultRepository.streamTargetIdByMapToJob(mapToJob.getId());
    }

    public List<String> findTargetIdsByMapToJob(MapToJob mapToJob, CursorPage cursorPage) {
        Pageable pageable = getPageable(cursorPage);
        return mapToResultRepository.findTargetIdByMapToJob(mapToJob, pageable);
    }
}
