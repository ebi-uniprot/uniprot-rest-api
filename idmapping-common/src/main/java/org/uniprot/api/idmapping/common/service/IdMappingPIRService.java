package org.uniprot.api.idmapping.common.service;

import java.util.Objects;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public abstract class IdMappingPIRService {
    private final int defaultPageSize;

    protected IdMappingPIRService(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public abstract IdMappingResult mapIds(IdMappingJobRequest request, String jobId);

    public QueryResult<IdMappingStringPair> queryResultPage(
            IdMappingPageRequest request, IdMappingResult result) {
        int pageSize = Objects.isNull(request.getSize()) ? defaultPageSize : request.getSize();

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(request.getCursor(), pageSize, result.getMappedIds().size());

        Stream<IdMappingStringPair> pageContent =
                result
                        .getMappedIds()
                        .subList(
                                cursorPage.getOffset().intValue(),
                                CursorPage.getNextOffset(cursorPage))
                        .stream();

        return QueryResult.<IdMappingStringPair>builder()
                .content(pageContent)
                .page(cursorPage)
                .extraOptions(IdMappingServiceUtils.getExtraOptions(result))
                .warnings(result.getWarnings())
                .build();
    }

    public QueryResult<IdMappingStringPair> queryResultAll(IdMappingResult result) {
        return QueryResult.<IdMappingStringPair>builder()
                .content(result.getMappedIds().stream())
                .extraOptions(IdMappingServiceUtils.getExtraOptions(result))
                .warnings(result.getWarnings())
                .build();
    }
}
