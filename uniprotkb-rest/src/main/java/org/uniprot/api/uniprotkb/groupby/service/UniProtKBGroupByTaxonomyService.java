package org.uniprot.api.uniprotkb.groupby.service;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.taxonomy.TaxonomyStreamRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UniProtKBGroupByTaxonomyService extends UniProtKBGroupByService<TaxonomyEntry> {
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] AND active:true";
    private final TaxonomyService taxonomyService;

    public UniProtKBGroupByTaxonomyService(
            TaxonomyService taxonomyService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.taxonomyService = taxonomyService;
    }

    @Override
    protected List<TaxonomyEntry> getChildren(String parent) {
        TaxonomyStreamRequest taxonomyStreamRequest = new TaxonomyStreamRequest();
        taxonomyStreamRequest.setQuery(
                isTopLevelSearch(parent) ? TOP_LEVEL_PARENT_QUERY : "parent:" + parent);
        return taxonomyService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }

    @Override
    protected Map<String, String> getFacetFields(List<TaxonomyEntry> entries) {
        String taxonomyIds =
                entries.stream()
                        .map(taxonomy -> String.valueOf(taxonomy.getTaxonId()))
                        .collect(Collectors.joining(","));
        return Map.of(
                FacetParams.FACET_FIELD, String.format("{!terms='%s'}taxonomy_id", taxonomyIds));
    }

    @Override
    protected GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<TaxonomyEntry> taxonomyEntries,
            List<TaxonomyEntry> ancestorEntries,
            String query) {
        Map<String, TaxonomyEntry> idEntryMap =
                taxonomyEntries.stream()
                        .collect(Collectors.toMap(this::getId, Function.identity()));
        return getGroupByResult(facetCounts, idEntryMap, ancestorEntries, query);
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