package org.uniprot.api.idmapping.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.uniprot.api.idmapping.controller.request.IdMappingSearchRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.StringUniProtKBEntryPair;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.Utils;

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
            IdMappingSearchRequest searchRequest) {
        // get the mapped ids from PIR
        IdMappingResult mappingResult = idMappingService.doPIRRequest(searchRequest);
        List<IdMappingStringPair> mappedIdPairs = mappingResult.getMappedIds();
        List<String> mappedIds =
                mappedIdPairs.stream()
                        .map(IdMappingStringPair::getValue)
                        .collect(Collectors.toList());
        List<Facet> facets = null;
        if (Utils.notNullNotEmpty(searchRequest.getFacets())) {
            SolrStreamFacetResponse solrStreamResponse =
                    searchBySolrStream(mappedIds, searchRequest);

            facets = solrStreamResponse.getFacets();
            if (Utils.notNullNotEmpty(searchRequest.getFacetFilter())) {
                // Apply Filter in PIR result
                List<String> solrFilteredIds = solrStreamResponse.getAccessions();
                mappedIdPairs =
                        mappedIdPairs.stream()
                                .filter(idPair -> solrFilteredIds.contains(idPair.getValue()))
                                .collect(Collectors.toList());
            }
        }

        // TODO add some checks like empty response from PIR
        int pageSize =
                Objects.isNull(searchRequest.getSize())
                        ? mappedIdPairs.size()
                        : searchRequest.getSize();

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(searchRequest.getCursor(), pageSize, mappedIdPairs.size());

        List<IdMappingStringPair> mappedIdsInPage =
                mappedIdPairs.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // extract ids to get entries from store
        Set<String> toIds =
                mappedIdsInPage.stream().map(Pair::getValue).collect(Collectors.toSet());
        Stream<UniProtKBEntry> entries = getEntries(new ArrayList<>(toIds));
        // accession -> entry map
        Map<String, UniProtKBEntry> idEntryMap = constructIdEntryMap(entries);
        // from -> uniprot entry
        Stream<StringUniProtKBEntryPair> result =
                mappedIdsInPage.stream()
                        .filter(mId -> idEntryMap.containsKey(mId.getValue()))
                        .map(mId -> convertToPair(mId, idEntryMap));

        return QueryResult.of(result, cursorPage, facets, null, mappingResult.getUnmappedIds());
    }

    @Override
    public String getFacetIdField() {
        return "accession_id";
    }

    private StringUniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return StringUniProtKBEntryPair.builder()
                .from(mId.getKey())
                .entry(idEntryMap.get(mId.getValue()))
                .build();
    }

    private Map<String, UniProtKBEntry> constructIdEntryMap(Stream<UniProtKBEntry> entries) {
        return entries.collect(
                Collectors.toMap(
                        entry -> entry.getPrimaryAccession().getValue(), Function.identity()));
    }
}
