package org.uniprot.api.idmapping.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.IdMappingSearchRequest;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry> {
    private final IDMappingPIRService idMappingService;

    public UniProtKBIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniProtKBEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) { // TODO Use UniprotKBFacetConfig
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
        this.idMappingService = idMappingService;
    }

    public QueryResult<Pair<String, UniProtKBEntry>> getMappedEntries(
            IdMappingSearchRequest searchRequest) {
        // get the mapped ids from PIR
        List<IdMappingStringPair> mappedIds =
                pirResponseConverter.convertToIDMappings(
                        idMappingService.doPIRRequest(searchRequest));
        // TODO add facet related code
        // TODO add some checks like empty response from PIR
        int pageSize =
                Objects.isNull(searchRequest.getSize())
                        ? mappedIds.size()
                        : searchRequest.getSize();

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(searchRequest.getCursor(), pageSize, mappedIds.size());

        List<IdMappingStringPair> mappedIdsInPage =
                mappedIds.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // extract id to get from store
        Set<String> toIds =
                mappedIdsInPage.stream().map(Pair::getValue).collect(Collectors.toSet());
        Stream<UniProtKBEntry> entries = getEntries(new ArrayList<>(toIds));
        // accession -> entry map
        Map<String, UniProtKBEntry> idEntryMap = constructIdEntryMap(entries);
        // from -> uniprot entry
        Stream<Pair<String, UniProtKBEntry>> result =
                mappedIds.stream()
                        .filter(mId -> idEntryMap.containsKey(mId.getValue()))
                        .map(mId -> convertToPair(mId, idEntryMap));
        return QueryResult.of(result, cursorPage, null, null);
    }

    private Pair<String, UniProtKBEntry> convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return new PairImpl<>(mId.getKey(), idEntryMap.get(mId.getValue()));
    }

    private Map<String, UniProtKBEntry> constructIdEntryMap(Stream<UniProtKBEntry> entries) {
        return entries.collect(
                Collectors.toMap(
                        entry -> entry.getPrimaryAccession().getValue(), Function.identity()));
    }
}
