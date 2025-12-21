package org.uniprot.api.idmapping.common.repository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.MapSolrParams;
import org.springframework.stereotype.Repository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.job.JobTask;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
import org.uniprot.store.search.SolrCollection;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class IdMappingRepository {

    private static final int MAX_ID_MAPPINGS_ALLOWED = 100_000;
    private final SolrClient uniProtKBSolrClient;
    private final SolrClient solrClient;

    public List<IdMappingStringPair> getAllMappingIds(
            SolrCollection collection, List<String> searchIds, String searchField, String idField)
            throws SolrServerException, IOException {
        String starQuery = "*:*";
        return getAllMappingIds(collection, searchIds, starQuery, searchField, idField);
    }

    public List<IdMappingStringPair> getAllMappingIds(
            SolrCollection collection,
            List<String> searchIds,
            String query,
            String searchField,
            String idField)
            throws SolrServerException, IOException {
        switch (collection) {
            case uniprot:
                return getAllMatchingIds(
                        uniProtKBSolrClient, collection, searchField, idField, searchIds, query);
            case uniparc:
                return getAllMatchingIds(
                        solrClient, collection, searchField, idField, searchIds, query);
            case uniref:
                // uniref id is big (23 char e-g UniRef100_UPI0000072840) 100_000 can not fit in
                // single request
                int sublistSize = Math.min(searchIds.size(), MAX_ID_MAPPINGS_ALLOWED / 2);
                var unirefSolrMappingList =
                        getAllMatchingIds(
                                solrClient,
                                collection,
                                searchField,
                                idField,
                                searchIds.subList(0, sublistSize),
                                query);
                if (searchIds.size() > sublistSize) {
                    unirefSolrMappingList.addAll(
                            getAllMatchingIds(
                                    solrClient,
                                    collection,
                                    searchField,
                                    idField,
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
        queryParamsMap.put("rows", "500010");
        queryParamsMap.put("fq", filteredTermQueryWithIds);
        MapSolrParams queryParams = new MapSolrParams(queryParamsMap);
        var results = client.query(collection.name(), queryParams).getResults();

        // special handling for search by proteome id. one proteome can have many uniparc/uniprot
        // entries
        if (JobTask.FROM_SEARCH_FIELD_MAP
                .get(IdMappingFieldConfig.PROTEOME_STR)
                .equalsIgnoreCase(searchField)) {
            return getIdMappingStringPairs(searchField, idField, searchIds, results);
        }

        return results.stream()
                .map(
                        document ->
                                new IdMappingStringPair(
                                        (String) document.getFirstValue(searchField),
                                        (String) document.getFirstValue(idField)))
                .collect(Collectors.toList());
    }

    private static List<IdMappingStringPair> getIdMappingStringPairs(
            String searchField, String idField, List<String> searchIds, SolrDocumentList results) {
        Set<String> searchIdSet = new HashSet<>(searchIds);
        List<IdMappingStringPair> idMappingStringPairs = new ArrayList<>();
        for (SolrDocument doc : results) {
            List<String> proteomeIds = (List<String>) doc.get(searchField);
            String idValue = (String) doc.getFirstValue(idField);
            for (String proteomeId : proteomeIds) {
                if (searchIdSet.contains(proteomeId.toLowerCase())) {
                    idMappingStringPairs.add(new IdMappingStringPair(proteomeId, idValue));
                }
            }
        }
        return idMappingStringPairs;
    }
}
