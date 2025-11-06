package org.uniprot.api.idmapping.common.service.impl;

import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
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
            String parentId, String query, IdMappingJob idMappingJob, List<String> entries) {
        if (GroupByService.isTopLevelSearch(parentId)) {
            return getFacetCounts(query, idMappingJob, entries);
        }
        List<FacetField.Count> facetCounts = getFacetCounts(query, idMappingJob, entries);
        String shortFormParent = groupByECService.getShortFormEc(parentId);
        return facetCounts.stream()
                .filter(facetCount -> shortFormParent.equals(facetCount.getName()))
                .toList();
    }
}
