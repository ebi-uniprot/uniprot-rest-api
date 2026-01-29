package org.uniprot.api.idmapping.common.repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.MapSolrParams;
import org.springframework.stereotype.Repository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.store.search.SolrCollection;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class IdMappingRepository {

    private static final int MAX_ID_MAPPINGS_ALLOWED = 100_000;
    private final SolrClient uniProtKBSolrClient;
    private final SolrClient solrClient;

    public List<IdMappingStringPair> getAllMappingIds(
            SolrCollection collection, List<String> searchIds)
            throws SolrServerException, IOException {
        String starQuery = "*:*";
        return getAllMappingIds(collection, searchIds, starQuery);
    }

    public List<IdMappingStringPair> getAllMappingIds(
            SolrCollection collection, List<String> searchIds, String query)
            throws SolrServerException, IOException {
        switch (collection) {
            case uniprot:
                return getAllMatchingIds(
                        uniProtKBSolrClient,
                        collection,
                        "accession_id",
                        "accession_id",
                        searchIds,
                        query);
            case uniparc:
                return getAllMatchingIds(solrClient, collection, "upi", "upi", searchIds, query);
            case uniref:
                // uniref id is big (23 char e-g UniRef100_UPI0000072840) 100_000 can not fit in
                // single request
                int sublistSize = Math.min(searchIds.size(), MAX_ID_MAPPINGS_ALLOWED / 2);
                var unirefSolrIdField = "id";
                var unirefSolrMappingList =
                        getAllMatchingIds(
                                solrClient,
                                collection,
                                unirefSolrIdField,
                                unirefSolrIdField,
                                searchIds.subList(0, sublistSize),
                                query);
                if (searchIds.size() > sublistSize) {
                    unirefSolrMappingList.addAll(
                            getAllMatchingIds(
                                    solrClient,
                                    collection,
                                    unirefSolrIdField,
                                    unirefSolrIdField,
                                    searchIds.subList(sublistSize, searchIds.size()),
                                    query));
                }
                return unirefSolrMappingList;
            default:
                return List.of();
        }
    }

    private List<IdMappingStringPair> getAllMatchingIds(
            SolrClient client,
            SolrCollection collection,
            String searchField,
            String idField,
            List<String> searchIds,
            String query)
            throws SolrServerException, IOException {

        var filteredTermQueryWithIds =
                String.format("({!terms f=%s}%s)", searchField, String.join(",", searchIds));
        var queryParamsMap = new HashMap<String, String>();
        queryParamsMap.put("q", query);
        queryParamsMap.put("fl", searchField + "," + idField);
        queryParamsMap.put("start", "0");
        queryParamsMap.put("rows", String.valueOf(searchIds.size()));
        queryParamsMap.put("fq", filteredTermQueryWithIds);
        MapSolrParams queryParams = new MapSolrParams(queryParamsMap);

        return client.query(collection.name(), queryParams).getResults().stream()
                .map(
                        document ->
                                new IdMappingStringPair(
                                        (String) document.getFirstValue(searchField),
                                        (String) document.getFirstValue(idField)))
                .collect(Collectors.toList());
    }
}
