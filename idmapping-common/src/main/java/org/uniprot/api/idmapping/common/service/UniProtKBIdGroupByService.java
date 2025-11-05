package org.uniprot.api.idmapping.common.service;

import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdMappingGroupByRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

import java.util.LinkedList;
import java.util.List;

public abstract class UniProtKBIdGroupByService<T> {
    private final GroupByService<T> groupByService;
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBIdGroupByService(GroupByService<T> groupByService, UniProtEntryService uniProtEntryService) {
        this.groupByService = groupByService;
        this.uniProtEntryService = uniProtEntryService;
    }

    public GroupByResult getGroupByResult(
            IdMappingJob idMappingJob, UniProtKBIdMappingGroupByRequest groupByRequest) {
        List<T> ancestors = new LinkedList<>();
        String parentId = groupByRequest.getParentId();
        List<T> lastChildEntries = groupByService.getInitialEntries(parentId);
        String query = groupByRequest.getQuery();
        List<FacetField.Count> parentFacetCounts =
                getInitialFacetCounts(parentId, query, idMappingJob, lastChildEntries);
        List<FacetField.Count> lastChildFacetCounts = parentFacetCounts;

        while (lastChildFacetCounts.size() == 1) {
            String currentId = lastChildFacetCounts.get(0).getName();
            List<T> childEntries = groupByService.getChildEntries(currentId);
            List<FacetField.Count> childFacetCounts = getFacetCounts(query, idMappingJob, childEntries);

            if (!childFacetCounts.isEmpty()) {
                groupByService.addToAncestors(ancestors, lastChildEntries, parentId, lastChildFacetCounts);
                lastChildFacetCounts = childFacetCounts;
                lastChildEntries = childEntries;
            } else {
                break;
            }
        }

        return groupByService.getGroupByResult(lastChildFacetCounts, lastChildEntries, ancestors, parentId, parentFacetCounts, query);
    }

    protected List<FacetField.Count> getInitialFacetCounts(String parentId, String query, IdMappingJob idMappingJob, List<T> entries) {
        return getFacetCounts(query, idMappingJob, entries);
    }

    protected List<FacetField.Count> getFacetCounts(String query, IdMappingJob idMappingJob, List<T> entries) {
        String processedQuery =
                UniProtQueryProcessor.newInstance(uniProtEntryService.getQueryProcessorConfig())
                        .processQuery(query);
        List<String> toIds = idMappingJob.getIdMappingResult().getMappedIds().stream().map(IdMappingStringPair::getTo).toList();
        List<FacetField> facetFields =
                uniProtEntryService.getFacets(processedQuery, groupByService.getFacetParams(entries), toIds);
       /* UniProtKBIdMappingSearchRequest searchRequest = new UniProtKBIdMappingSearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setFacets(groupByService.getFacetParams(entries));
        SolrStreamFacetResponse solrStreamFacetResponse = uniProtKBIdService.searchMappedIdsFacetsBySearchRequest(searchRequest, toIds);
        List<Facet> facets = solrStreamFacetResponse.getFacets();

        if (!facets.isEmpty() && facets.get(0).getValues() != null) {
            return facets.get(0).getValues().stream()
                    .filter(facetItem -> facetItem.getCount() > 0)
                    .map(facetItem -> new FacetField.Count(new FacetField(facetItem.getLabel()), facetItem.getValue(), facetItem.getCount()))
                    .toList();
        }

        return List.of();*/
        if (!facetFields.isEmpty() && facetFields.get(0).getValues() != null) {
            return facetFields.get(0).getValues().stream()
                    .filter(count -> count.getCount() > 0)
                    .toList();
        }

        return List.of();
    }
}
