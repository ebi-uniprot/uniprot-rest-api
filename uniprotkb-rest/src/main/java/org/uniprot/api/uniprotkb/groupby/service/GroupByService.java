package org.uniprot.api.uniprotkb.groupby.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.uniprotkb.groupby.model.*;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class GroupByService<T> {
    private final UniProtEntryService uniProtEntryService;

    GroupByService(UniProtEntryService uniProtEntryService) {
        this.uniProtEntryService = uniProtEntryService;
    }

    public GroupByResult getGroupByResult(String query, String parentId) {
        List<FacetField.Count> lastChildFacetCounts = List.of();
        List<T> lastChildEntries = List.of();
        String currentId = parentId;
        List<T> ancestors = new LinkedList<>();

        do {
            List<T> childEntries = getChildEntries(currentId);
            List<FacetField.Count> childFacetCounts = getFacetCounts(query, childEntries);

            if (!childFacetCounts.isEmpty()) {
                addToAncestors(ancestors, lastChildEntries, parentId, lastChildFacetCounts);
                lastChildFacetCounts = childFacetCounts;
                lastChildEntries = childEntries;
                currentId = lastChildFacetCounts.get(0).getName();
            } else {
                break;
            }

        } while (lastChildFacetCounts.size() == 1);

        return getGroupByResult(lastChildFacetCounts, lastChildEntries, ancestors, parentId, query);
    }

    protected static boolean isTopLevelSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    protected void addToAncestors(
            List<T> ancestors, List<T> entries, String parent, List<FacetField.Count> facetCounts) {
        if (!facetCounts.isEmpty()) {
            String facetId = getFacetId(facetCounts.get(0));
            if (!Objects.equals(parent, facetId) && !entries.isEmpty()) {
                ancestors.add(
                        entries.stream()
                                .filter(t -> facetId.equals(getId(t)))
                                .findAny()
                                .orElseThrow());
            }
        }
    }

    private List<FacetField.Count> getFacetCounts(String query, List<T> entries) {
        List<FacetField> facetFields =
                uniProtEntryService.getFacets(query, getFacetParams(entries));

        if (!facetFields.isEmpty() && facetFields.get(0).getValues() != null) {
            return facetFields.get(0).getValues().stream()
                    .filter(count -> count.getCount() > 0)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    protected boolean hasChildren(FacetField.Count count, String queryStr) {
        List<T> children = getChildEntries(count.getName());
        return !getFacetCounts(queryStr, children).isEmpty();
    }

    protected abstract GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<T> entries,
            List<T> ancestorEntries,
            String parentId,
            String query);

    protected GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            Map<String, T> idEntryMap,
            List<T> ancestorEntries,
            String parentId,
            String query) {
        List<Group> groups =
                facetCounts.stream()
                        .map(fc -> getGroup(fc, idEntryMap.get(getFacetId(fc)), query))
                        .sorted(Group.SORT_BY_LABEL_IGNORE_CASE)
                        .collect(Collectors.toList());
        List<Ancestor> ancestors =
                ancestorEntries.stream().map(this::getAncestor).collect(Collectors.toList());
        Parent parent = getParentInfo(parentId, groups);

        return new GroupByResult(ancestors, groups, parent);
    }

    private Parent getParentInfo(String parentId, List<Group> groups){
        if (isTopLevelSearch(parentId)) {
            return null;
        }
        long count = groups.stream().mapToLong(Group::getCount).sum();
        return ParentImpl.builder().label(getLabel(getEntry(parentId))).count(count).build();
    }

    protected abstract T getEntry(String parentId);

    protected String getFacetId(FacetField.Count fc) {
        return fc.getName();
    }

    protected Ancestor getAncestor(T t) {
        return AncestorImpl.builder().id(getId(t)).label(getLabel(t)).build();
    }

    protected Group getGroup(FacetField.Count count, T entry, String queryStr) {
        return GroupImpl.builder()
                .id(getId(entry))
                .label(getLabel(entry))
                .count(count.getCount())
                .expandable(hasChildren(count, queryStr))
                .build();
    }

    protected abstract String getId(T entry);

    protected abstract String getLabel(T entry);

    protected abstract List<T> getChildEntries(String parent);

    protected abstract Map<String, String> getFacetParams(List<T> entries);
}
