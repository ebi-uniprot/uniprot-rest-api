package org.uniprot.api.idmapping.common.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdGroupByRequest;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class UniProtKBIdGroupByService<T> {
    private final GroupByService<T> groupByService;
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBIdGroupByService(
            GroupByService<T> groupByService, UniProtEntryService uniProtEntryService) {
        this.groupByService = groupByService;
        this.uniProtEntryService = uniProtEntryService;
    }

    public GroupByResult getGroupByResult(UniProtKBIdGroupByRequest groupByRequest) {
        String givenParent = groupByRequest.getParent();
        // initial graph (or tree) nodes for which we should be searching for
        List<T> lastChildEntries = groupByService.getInitialEntries(givenParent);
        // facet results for above nodes
        List<FacetField.Count> parentFacetCounts =
                getInitialFacetCounts(groupByRequest, lastChildEntries);
        List<FacetField.Count> lastChildFacetCounts = parentFacetCounts;
        List<T> ancestors = new LinkedList<>();
        log.debug(
                "For parent %s, number of  group by results got:%d"
                        .formatted(givenParent, parentFacetCounts.size()));

        // now we should see whether we have results such that, we have only one node, and it is
        // expandable further
        // in such cases we have to expand as long as possible such that we don't have such as final
        // results
        while (lastChildFacetCounts.size() == 1) {
            String currentParent = lastChildFacetCounts.get(0).getName();
            List<T> childEntries = groupByService.getChildEntries(currentParent);
            List<FacetField.Count> childFacetCounts = getFacetCounts(groupByRequest, childEntries);

            // it can be drilled down further
            if (isExpandable(childFacetCounts)) {
                // add previous results
                log.debug(
                        "Group by results with parent %s is just a single node and expandable"
                                .formatted(currentParent));
                groupByService.addToAncestors(
                        ancestors, lastChildEntries, givenParent, lastChildFacetCounts);
                lastChildFacetCounts = childFacetCounts;
                lastChildEntries = childEntries;
            } else {
                break;
            }
        }

        return groupByService.getGroupByResult(
                lastChildFacetCounts,
                lastChildEntries,
                ancestors,
                givenParent,
                parentFacetCounts,
                groupByRequest.getQuery());
    }

    private boolean isExpandable(List<FacetField.Count> childFacetCounts) {
        return !childFacetCounts.isEmpty();
    }

    protected List<FacetField.Count> getInitialFacetCounts(
            UniProtKBIdGroupByRequest groupByRequest, List<T> entries) {
        return getFacetCounts(groupByRequest, entries);
    }

    protected List<FacetField.Count> getFacetCounts(
            UniProtKBIdGroupByRequest groupByRequest, List<T> entries) {
        String query = groupByRequest.getQuery();
        String processedQuery =
                UniProtQueryProcessor.newInstance(uniProtEntryService.getQueryProcessorConfig())
                        .processQuery(query);
        Map<String, String> facetParams = groupByService.getFacetParams(entries);
        List<String> ids = groupByRequest.getIds();
        List<FacetField> facetFields =
                uniProtEntryService.getFacets(processedQuery, facetParams, ids);
        return getFacetCounts(facetFields);
    }

    private List<FacetField.Count> getFacetCounts(List<FacetField> facetFields) {
        if (!facetFields.isEmpty() && facetFields.get(0).getValues() != null) {
            return facetFields.get(0).getValues().stream()
                    .filter(count -> count.getCount() > 0)
                    .toList();
        }

        return List.of();
    }
}
