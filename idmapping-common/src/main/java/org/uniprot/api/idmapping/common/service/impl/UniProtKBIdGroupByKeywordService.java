package org.uniprot.api.idmapping.common.service.impl;

import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.UniProtKBIdGroupByService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByKeywordService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.cv.keyword.KeywordEntry;

@Service
public class UniProtKBIdGroupByKeywordService extends UniProtKBIdGroupByService<KeywordEntry> {
    private final GroupByKeywordService groupByKeywordService;

    public UniProtKBIdGroupByKeywordService(
            GroupByKeywordService groupByKeywordService, UniProtEntryService uniProtEntryService) {
        super(groupByKeywordService, uniProtEntryService);
        this.groupByKeywordService = groupByKeywordService;
    }

    @Override
    protected List<FacetField.Count> getFacetCounts(
            String query, IdMappingJob idMappingJob, List<KeywordEntry> entries) {
        List<FacetField.Count> facetCounts = super.getFacetCounts(query, idMappingJob, entries);
        return groupByKeywordService.getKeywordFacetCounts(facetCounts);
    }
}
