package org.uniprot.api.mapto.common.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToEntryId;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToPageRequest;

@Service
public class MapToTargetIdService {
    private final MapToJobService mapToJobService;
    private final MapToResultService mapToResultService;
    private final Integer defaultPageSize;

    protected MapToTargetIdService(
            MapToJobService mapToJobService,
            MapToResultService mapToResultService,
            @Value("${search.request.converter.defaultRestPageSize:#{null}}")
                    Integer defaultPageSize) {
        this.mapToJobService = mapToJobService;
        this.mapToResultService = mapToResultService;
        this.defaultPageSize = defaultPageSize;
    }

    public QueryResult<MapToEntryId> getMapToEntryIds(MapToPageRequest request, String jobId) {
        int pageSize = Objects.isNull(request.getSize()) ? defaultPageSize : request.getSize();

        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(request.getCursor(), pageSize, mapToJob.getTotalTargetIds());
        List<String> mapToJobIds = mapToResultService.findTargetIdsByMapToJob(mapToJob, cursorPage);
        Stream<MapToEntryId> pageContent = mapToJobIds.stream().map(MapToEntryId::new);

        return QueryResult.<MapToEntryId>builder().content(pageContent).page(cursorPage).build();
    }

    public List<MapToEntryId> getMapToEntryIds(String jobId) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        List<String> allTargetIdsByMapToJob =
                mapToResultService.findAllTargetIdsByMapToJob(mapToJob);
        return allTargetIdsByMapToJob.stream().map(MapToEntryId::new).toList();
    }
}
