package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.util.List;
import java.util.stream.Collectors;

public abstract class UniProtViewByService<T> {
    private final UniProtEntryService uniProtEntryService;

    UniProtViewByService(UniProtEntryService uniProtEntryService) {
        this.uniProtEntryService = uniProtEntryService;
    }

    public List<ViewBy> getViewBys(String query, String parent) {
        List<FacetField.Count> facetCounts = List.of();
        List<T> entries = List.of();
        String id = parent;

        do {
            List<T> childEntries = getChildren(id);
            List<FacetField.Count> childFacetCounts = getFacetCounts(query, childEntries);

            if (!childFacetCounts.isEmpty()) {
                facetCounts = childFacetCounts;
                entries = childEntries;
                id = facetCounts.get(0).getName();
            } else {
                break;
            }

        } while (facetCounts.size() == 1 && isTopLevelSearch(parent));

        return getViewBys(facetCounts, entries, query);
    }

    protected boolean hasChildren(FacetField.Count count, String queryStr) {
        List<T> children = getChildren(count.getName());
        return !getFacetCounts(queryStr, children).isEmpty();
    }

    private List<FacetField.Count> getFacetCounts(String query, List<T> entries) {
        if (!entries.isEmpty()) {
            List<FacetField> facetFields = uniProtEntryService.getFacets(query, getFacetFields(entries));

            if (!facetFields.isEmpty()) {
                return facetFields.get(0).getValues().stream()
                        .filter(count -> count.getCount() > 0)
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }

    protected static boolean isTopLevelSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    protected abstract List<T> getChildren(String parent);

    protected abstract String getFacetFields(List<T> entries);

    protected abstract List<ViewBy> getViewBys(List<FacetField.Count> facetCounts, List<T> entries, String query);
}
