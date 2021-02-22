package org.uniprot.api.idmapping.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.StringUniProtKBEntryPair;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, StringUniProtKBEntryPair> {
    private final IDMappingPIRService idMappingService;

    public UniProtKBIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniProtKBEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            UniprotKBFacetConfig facetConfig) {
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
        this.idMappingService = idMappingService;
    }

    public QueryResult<StringUniProtKBEntryPair> getMappedEntries(
            UniProtKBIdMappingSearchRequest searchRequest) {
        // get the mapped ids from PIR
        IdMappingResult mappingResult = idMappingService.doPIRRequest(searchRequest);
        List<IdMappingStringPair> mappedIdPairs = mappingResult.getMappedIds();
        List<Facet> facets = null;
        if (needSearchInSolr(searchRequest)) {

            List<String> toIds =
                    mappedIdPairs.stream()
                            .map(IdMappingStringPair::getTo)
                            .collect(Collectors.toList());

            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(toIds, searchRequest);

            facets = solrStreamResponse.getFacets();

            List<String> solrToIds = solrStreamResponse.getAccessions();
            if (Utils.notNullNotEmpty(searchRequest.getQuery())) {
                // Apply Filter in PIR result
                mappedIdPairs =
                        mappedIdPairs.stream()
                                .filter(idPair -> solrToIds.contains(idPair.getTo()))
                                .collect(Collectors.toList());
            }

            if (Utils.notNullNotEmpty(searchRequest.getSort())) {
                // create a Map<To,List<From>>
                Map<String, List<String>> toMap =
                        mappedIdPairs.stream()
                                .collect(
                                        Collectors.groupingBy(
                                                IdMappingStringPair::getTo,
                                                Collectors.mapping(
                                                        IdMappingStringPair::getFrom,
                                                        Collectors.toList())));
                mappedIdPairs =
                        solrToIds.stream()
                                .flatMap(
                                        to ->
                                                toMap.get(to).stream()
                                                        .map(
                                                                from ->
                                                                        new IdMappingStringPair(
                                                                                from, to)))
                                .collect(Collectors.toList());
            }
        }

        int pageSize =
                Objects.isNull(searchRequest.getSize())
                        ? getDefaultPageSize()
                        : searchRequest.getSize();

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(searchRequest.getCursor(), pageSize, mappedIdPairs.size());

        List<IdMappingStringPair> mappedIdsInPage =
                mappedIdPairs.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // extract ids to get entries from store
        Set<String> toIds =
                mappedIdsInPage.stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toSet());
        Stream<UniProtKBEntry> entries = getEntries(new ArrayList<>(toIds));
        // accession -> entry map
        Map<String, UniProtKBEntry> idEntryMap = constructIdEntryMap(entries);
        // from -> uniprot entry
        Stream<StringUniProtKBEntryPair> result =
                mappedIdsInPage.stream()
                        .filter(mId -> idEntryMap.containsKey(mId.getTo()))
                        .map(mId -> convertToPair(mId, idEntryMap));

        return QueryResult.of(result, cursorPage, facets, null, mappingResult.getUnmappedIds());
    }

    @Override
    public String getSolrIdField() {
        return "accession_id";
    }

    @Override
    public UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    private StringUniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return StringUniProtKBEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    private Map<String, UniProtKBEntry> constructIdEntryMap(Stream<UniProtKBEntry> entries) {
        return entries.collect(
                Collectors.toMap(
                        entry -> entry.getPrimaryAccession().getValue(), Function.identity()));
    }

    private boolean needSearchInSolr(UniProtKBIdMappingSearchRequest searchRequest) {
        return Utils.notNullNotEmpty(searchRequest.getQuery())
                || Utils.notNullNotEmpty(searchRequest.getFacets())
                || Utils.notNullNotEmpty(searchRequest.getSort());
    }
}
