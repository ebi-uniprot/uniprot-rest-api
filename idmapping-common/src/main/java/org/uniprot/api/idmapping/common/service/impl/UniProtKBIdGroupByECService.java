package org.uniprot.api.idmapping.common.service.impl;

import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdGroupByRequest;
import org.uniprot.api.idmapping.common.service.UniProtKBIdGroupByService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByECService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

@Service
public class UniProtKBIdGroupByECService extends UniProtKBIdGroupByService<String> {
    private final GroupByECService groupByECService;

    public UniProtKBIdGroupByECService(
            GroupByECService groupByECService, UniProtEntryService uniProtEntryService) {
        super(groupByECService, uniProtEntryService);
        this.groupByECService = groupByECService;
    }

    @Override
    protected List<FacetField.Count> getInitialFacetCounts(
            UniProtKBIdGroupByRequest groupByRequest, List<String> entries) {
        String parent = groupByRequest.getParent();
        if (GroupByService.isEmptyParent(parent)) {
            return getFacetCounts(groupByRequest, entries);
        }
        List<FacetField.Count> facetCounts = getFacetCounts(groupByRequest, entries);
        String shortFormParent = groupByECService.getShortFormEc(parent);
        return facetCounts.stream()
                .filter(facetCount -> shortFormParent.equals(facetCount.getName()))
                .toList();
    }
}
