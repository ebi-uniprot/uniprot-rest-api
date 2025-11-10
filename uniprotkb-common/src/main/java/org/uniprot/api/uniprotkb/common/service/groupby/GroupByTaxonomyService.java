package org.uniprot.api.uniprotkb.common.service.groupby;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.common.taxonomy.request.TaxonomyStreamRequest;
import org.uniprot.api.support.data.common.taxonomy.service.TaxonomyService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

@Service
public class GroupByTaxonomyService extends GroupByService<TaxonomyEntry> {
    public static final String TOP_LEVEL_TAXONOMY_PARENT_QUERY = "-parent:[* TO *] AND active:true";
    private final TaxonomyService taxonomyService;

    public GroupByTaxonomyService(
            TaxonomyService taxonomyService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.taxonomyService = taxonomyService;
    }

    @Override
    public List<TaxonomyEntry> getChildEntries(String parent) {
        TaxonomyStreamRequest taxonomyStreamRequest = new TaxonomyStreamRequest();
        taxonomyStreamRequest.setQuery(
                isEmptyParent(parent) ? TOP_LEVEL_TAXONOMY_PARENT_QUERY : "parent:" + parent);
        return taxonomyService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getFacetParams(List<TaxonomyEntry> entries) {
        String taxonomyIds =
                entries.stream()
                        .map(taxonomy -> String.valueOf(taxonomy.getTaxonId()))
                        .collect(Collectors.joining(","));
        return Map.of(
                FacetParams.FACET_FIELD, String.format("{!terms='%s'}taxonomy_id", taxonomyIds));
    }

    @Override
    public GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<TaxonomyEntry> taxonomyEntries,
            List<TaxonomyEntry> ancestorEntries,
            String parentId,
            List<FacetField.Count> parentFacetCounts,
            String query) {
        Map<String, TaxonomyEntry> idEntryMap =
                taxonomyEntries.stream()
                        .collect(Collectors.toMap(this::getId, Function.identity()));
        return getGroupByResult(
                facetCounts, idEntryMap, ancestorEntries, parentId, parentFacetCounts, query);
    }

    @Override
    protected TaxonomyEntry getEntryById(String id) {
        return taxonomyService.findById(Long.parseLong(id));
    }

    @Override
    protected String getId(TaxonomyEntry entry) {
        return String.valueOf(entry.getTaxonId());
    }

    @Override
    protected String getLabel(TaxonomyEntry entry) {
        return entry.getScientificName();
    }
}
