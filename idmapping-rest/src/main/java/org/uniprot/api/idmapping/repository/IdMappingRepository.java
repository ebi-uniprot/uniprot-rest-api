package org.uniprot.api.idmapping.repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.MapSolrParams;
import org.springframework.stereotype.Repository;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.store.search.SolrCollection;

@Repository
@AllArgsConstructor
public class IdMappingRepository {

    private static final int MAX_ID_MAPPINGS_ALLOWED = 100_000;
    private final SolrClient uniProtKBSolrClient;
    private final SolrClient solrClient;

    public List<IdMappingStringPair> getAllMappingIds(
            SolrCollection collection, @Size(max = MAX_ID_MAPPINGS_ALLOWED) List<String> searchIds)
            throws SolrServerException, IOException {
        switch (collection) {
            case uniprot:
                return getAllMatchingIds(
                        uniProtKBSolrClient, collection, "all_ids", "accession_id", searchIds);
            case uniparc:
                return getAllMatchingIds(solrClient, collection, "upi", "upi", searchIds);
            case uniref:
                // uniref id is big (23 char e-g UniRef100_UPI0000072840) 100_000 can not fit in
                // single request
                var sublistSize = Math.min(searchIds.size(), MAX_ID_MAPPINGS_ALLOWED / 2);
                var ret =
                        getAllMatchingIds(
                                solrClient,
                                collection,
                                "id",
                                "id",
                                searchIds.subList(0, sublistSize));
                if (searchIds.size() > sublistSize)
                    ret.addAll(
                            getAllMatchingIds(
                                    solrClient,
                                    collection,
                                    "id",
                                    "id",
                                    searchIds.subList(sublistSize, searchIds.size())));
                return ret;
            default:
                return List.of();
        }
    }

    private List<IdMappingStringPair> getAllMatchingIds(
            SolrClient client,
            SolrCollection collection,
            String searchField,
            String idField,
            @Size(max = MAX_ID_MAPPINGS_ALLOWED) List<String> searchIds)
            throws SolrServerException, IOException {

        var filteredQuery =
                String.format("({!terms f=%s}%s)", searchField, String.join(",", searchIds));
        final Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("q", "*:*");
        queryParamMap.put("fl", searchField + "," + idField);
        queryParamMap.put("start", "0");
        queryParamMap.put("rows", "" + searchIds.size());
        queryParamMap.put("fq", filteredQuery);
        MapSolrParams queryParams = new MapSolrParams(queryParamMap);

        return client.query(collection.name(), queryParams).getResults().stream()
                .map(
                        document ->
                                new IdMappingStringPair(
                                        (String) document.getFirstValue(searchField),
                                        (String) document.getFirstValue(idField)))
                .collect(Collectors.toList());
    }
}
