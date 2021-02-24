package org.uniprot.api.idmapping.service;

import java.util.Objects;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public interface IDMappingPIRService {
    IdMappingResult mapIds(IdMappingBasicRequest request);

    default QueryResult<IdMappingStringPair> queryResultPage(
            IdMappingBasicRequest request, IdMappingResult result) {
        int pageSize =
                Objects.isNull(request.getSize())
                        ? result.getMappedIds().size()
                        : request.getSize();

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
                null,
                result.getUnmappedIds());
    }

    default QueryResult<IdMappingStringPair> queryResultAll(IdMappingResult result) {
        return QueryResult.of(
                result.getMappedIds().stream(), null, null, null, result.getUnmappedIds());
    }
}
