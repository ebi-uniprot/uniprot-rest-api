package org.uniprot.api.uniprotkb.groupby.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.uniprotkb.groupby.model.*;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

public abstract class UniProtKBGroupByService<T> {
    private final UniProtEntryService uniProtEntryService;

    UniProtKBGroupByService(UniProtEntryService uniProtEntryService) {
        this.uniProtEntryService = uniProtEntryService;
    }

    public GroupByResult getGroups(String query, String parent) {
        List<FacetField.Count> facetCounts = List.of();
        List<T> entries = List.of();
        String id = parent;
        List<T> ancestors = new LinkedList<>();

        do {
            List<T> childEntries = getChildren(id);
            List<FacetField.Count> childFacetCounts = getFacetCounts(query, childEntries);

            if (!childFacetCounts.isEmpty()) {
                addToAncestors(ancestors, entries, parent, id);
                facetCounts = childFacetCounts;
                entries = childEntries;
                id = facetCounts.get(0).getName();
            } else {
                break;
            }

        } while (facetCounts.size() == 1);

        return getGroups(facetCounts, entries, ancestors, query);
    }

    protected static boolean isTopLevelSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    protected void addToAncestors(List<T> ancestors, List<T> entries, String parent, String id) {
        if (!Objects.equals(parent, id) && !entries.isEmpty()) {
            ancestors.add(entries.get(0));
        }
    }

    private List<FacetField.Count> getFacetCounts(String query, List<T> entries) {
        List<FacetField> facetFields =
                uniProtEntryService.getFacets(query, getFacetFields(entries));

        if (!facetFields.isEmpty() && facetFields.get(0).getValues() != null) {
            return facetFields.get(0).getValues().stream()
                    .filter(count -> count.getCount() > 0)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    protected boolean hasChildren(FacetField.Count count, String queryStr) {
        List<T> children = getChildren(count.getName());
        return !getFacetCounts(queryStr, children).isEmpty();
    }

    private GroupByResult getGroups(
            List<FacetField.Count> facetCounts,
            List<T> entries,
            List<T> ancestorEntries,
            String query) {
        Map<String, T> idEntryMap = getEntryMap(entries, facetCounts);
        List<Group> groups =
                facetCounts.stream()
                        .map(fc -> getGroup(fc, idEntryMap.get(getFacetName(fc)), query))
                        .sorted(Group.SORT_BY_LABEL_IGNORE_CASE)
                        .collect(Collectors.toList());
        List<Ancestor> ancestors =
                ancestorEntries.stream().map(this::getAncestor).collect(Collectors.toList());
        return new GroupByResult(ancestors, groups);
    }

    protected Map<String, T> getEntryMap(List<T> entries, List<FacetField.Count> facetCounts) {
        return entries.stream().collect(Collectors.toMap(this::getId, Function.identity()));
    }

    protected String getFacetName(FacetField.Count fc) {
        return fc.getName();
    }

    private Ancestor getAncestor(T t) {
        return AncestorImpl.builder().id(getId(t)).label(getLabel(t)).build();
    }

    private Group getGroup(FacetField.Count count, T entry, String queryStr) {
        return GroupImpl.builder()
                .id(getId(entry))
                .label(getLabel(entry))
                .count(count.getCount())
                .expand(hasChildren(count, queryStr))
                .build();
    }

    protected abstract String getId(T entry);

    protected abstract String getLabel(T entry);

    protected abstract List<T> getChildren(String parent);

    protected abstract Map<String, String> getFacetFields(List<T> entries);
}
