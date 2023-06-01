package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.taxonomy.TaxonomyStreamRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByImpl;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UniProtKBViewByTaxonomyService implements UniProtViewByService {
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] AND active:true";
    public static final String URL_PHRASE = "/taxonomy/";
    private final TaxonomyService taxonomyService;
    private final UniProtEntryService uniProtEntryService;

    public UniProtKBViewByTaxonomyService(TaxonomyService taxonomyService, UniProtEntryService uniProtEntryService) {
        this.uniProtEntryService = uniProtEntryService;
        this.taxonomyService = taxonomyService;
    }

    @Override
    public List<ViewBy> getViewBys(String queryStr, String parent) {
        List<FacetField.Count> facetCounts = List.of();
        List<TaxonomyEntry> taxonomyEntries = List.of();
        String taxonomyId = parent;

        do {
            List<TaxonomyEntry> childTaxonomyEntries = getChildren(taxonomyId);
            List<FacetField.Count> childFacetCounts = getFacetCounts(queryStr, childTaxonomyEntries);

            if (!childFacetCounts.isEmpty()) {
                facetCounts = childFacetCounts;
                taxonomyEntries = childTaxonomyEntries;
                taxonomyId = facetCounts.get(0).getName();
            } else {
                break;
            }

        } while (facetCounts.size() == 1 && isTopLevelSearch(parent));

        return getViewBys(facetCounts, taxonomyEntries, queryStr);
    }

    private static boolean isTopLevelSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    private List<ViewBy> getViewBys(
            List<FacetField.Count> facetCounts,
            List<TaxonomyEntry> taxonomyEntries,
            String queryStr) {
        Map<Long, TaxonomyEntry> taxIdMap =
                taxonomyEntries.stream()
                        .collect(Collectors.toMap(Taxonomy::getTaxonId, Function.identity()));

        return facetCounts.stream()
                .map(
                        fc ->
                                getViewBy(
                                        fc,
                                        taxIdMap.get(Long.parseLong(fc.getName())),
                                        queryStr))
                .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                .collect(Collectors.toList());
    }

    private ViewBy getViewBy(
            FacetField.Count count, TaxonomyEntry taxonomyEntry, String queryStr) {
        return ViewByImpl.builder().id(count.getName())
                .count(count.getCount())
                .link(URL_PHRASE + count.getName())
                .label(taxonomyEntry.getScientificName())
                .expand(hasChildren(count, queryStr))
                .build();
    }

    private boolean hasChildren(FacetField.Count count, String queryStr) {
        List<TaxonomyEntry> children = getChildren(count.getName());
        return !getFacetCounts(queryStr, children).isEmpty();
    }

    private List<FacetField.Count> getFacetCounts(String query, List<TaxonomyEntry> taxonomyEntries) {
        String facetItems = taxonomyEntries.stream()
                .map(taxonomy -> String.valueOf(taxonomy.getTaxonId()))
                .collect(Collectors.joining(","));
        List<FacetField> facetFields = uniProtEntryService
                .getFacets(query, String.format("{!terms='%s'}taxonomy_id", facetItems));

        if (!facetFields.isEmpty() && facetFields.get(0).getValues() != null) {
            return facetFields.get(0).getValues().stream()
                    .filter(count -> count.getCount() > 0)
                    .collect(Collectors.toList());
        }

        return List.of();
    }


    private List<TaxonomyEntry> getChildren(String taxId) {
        TaxonomyStreamRequest taxonomyStreamRequest = new TaxonomyStreamRequest();
        taxonomyStreamRequest.setQuery(isTopLevelSearch(taxId) ? TOP_LEVEL_PARENT_QUERY : "parent:" + taxId);
        return taxonomyService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }
}
