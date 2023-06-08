package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.taxonomy.TaxonomyStreamRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UniProtKBViewByTaxonomyService extends UniProtKBViewByService<TaxonomyEntry> {
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] AND active:true";
    private final TaxonomyService taxonomyService;

    public UniProtKBViewByTaxonomyService(
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
    protected String getId(TaxonomyEntry entry) {
        return String.valueOf(entry.getTaxonId());
    }

    @Override
    protected String getLabel(TaxonomyEntry entry) {
        return entry.getScientificName();
    }

    /*@Override
    protected ViewByResult getViewBys(
            List<FacetField.Count> facetCounts, List<TaxonomyEntry> entries, List<TaxonomyEntry> ancestors, String query) {
        Map<Long, TaxonomyEntry> taxIdMap =
                entries.stream()
                        .collect(Collectors.toMap(Taxonomy::getTaxonId, Function.identity()));

        return new ViewByResult(getAncestors(ancestors), facetCounts.stream()
                .map(fc -> getViewBy(fc, taxIdMap.get(Long.parseLong(fc.getName())), query))
                .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                .collect(Collectors.toList()));
    }

    private List<Ancestor> getAncestors(List<TaxonomyEntry> ancestors) {
        return ancestors.stream()
                .map(te -> AncestorImpl.builder().id(String.valueOf(te.getTaxonId())).label(te.getScientificName()).build())
                .collect(Collectors.toList());
    }

    private ViewBy getViewBy(FacetField.Count count, TaxonomyEntry taxonomyEntry, String queryStr) {
        return ViewByImpl.builder()
                .id(count.getName())
                .label(taxonomyEntry.getScientificName())
                .count(count.getCount())
                .expand(hasChildren(count, queryStr))
                .build();
    }*/
}
