package org.uniprot.api.idmapping.service;

import java.util.Objects;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.idmapping.controller.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

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

        return QueryResult.of(
                result.getMappedIds()
                        .subList(
                                cursorPage.getOffset().intValue(),
                                CursorPage.getNextOffset(cursorPage))
                        .stream(),
                cursorPage,
                null,
                result.getUnmappedIds(),
                result.getWarnings());
    }

    public QueryResult<IdMappingStringPair> queryResultAll(IdMappingResult result) {
        return QueryResult.of(
                result.getMappedIds().stream(),
                null,
                null,
                result.getUnmappedIds(),
                result.getWarnings());
    }
}
