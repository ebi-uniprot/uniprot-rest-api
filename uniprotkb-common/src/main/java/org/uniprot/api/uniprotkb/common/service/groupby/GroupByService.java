package org.uniprot.api.uniprotkb.common.service.groupby;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.uniprotkb.common.service.groupby.model.*;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

public abstract class GroupByService<T> {
    private final UniProtEntryService uniProtEntryService;

    GroupByService(UniProtEntryService uniProtEntryService) {
        this.uniProtEntryService = uniProtEntryService;
    }

    public GroupByResult getGroupByResult(String query, String parentId) {
        List<T> ancestors = new LinkedList<>();
        List<T> lastChildEntries = getInitialEntries(parentId);
        List<FacetField.Count> parentFacetCounts =
                getInitialFacetCounts(parentId, query, lastChildEntries);
        List<FacetField.Count> lastChildFacetCounts = parentFacetCounts;

        while (lastChildFacetCounts.size() == 1) {
            String currentId = lastChildFacetCounts.get(0).getName();
            List<T> childEntries = getChildEntries(currentId);
            List<FacetField.Count> childFacetCounts = getFacetCounts(query, childEntries);

            if (!childFacetCounts.isEmpty()) {
                addToAncestors(ancestors, lastChildEntries, parentId, lastChildFacetCounts);
                lastChildFacetCounts = childFacetCounts;
                lastChildEntries = childEntries;
            } else {
                break;
            }
        }

        return getGroupByResult(
                lastChildFacetCounts,
                lastChildEntries,
                ancestors,
                parentId,
                parentFacetCounts,
                query);
    }

    protected static boolean isTopLevelSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    protected void addToAncestors(
            List<T> ancestors, List<T> entries, String parent, List<FacetField.Count> facetCounts) {
        if (!facetCounts.isEmpty()) {
            String facetId = getFacetId(facetCounts.get(0));
            if (!areEqualIds(parent, facetId) && !entries.isEmpty()) {
                ancestors.add(
                        entries.stream()
                                .filter(t -> facetId.equals(getId(t)))
                                .findAny()
                                .orElseThrow());
            }
        }
    }

    protected boolean areEqualIds(String parent, String facetId) {
        return Objects.equals(parent, facetId);
    }

    List<T> getInitialEntries(String parentId) {
        if (!isTopLevelSearch(parentId)) {
            return List.of(getEntryById(parentId));
        }
        return getChildEntries(parentId);
    }

    List<FacetField.Count> getInitialFacetCounts(String parentId, String query, List<T> entries) {
        return getFacetCounts(query, entries);
    }

    protected List<FacetField.Count> getFacetCounts(String query, List<T> entries) {
        String processedQuery =
                UniProtQueryProcessor.newInstance(uniProtEntryService.getQueryProcessorConfig())
                        .processQuery(query);
        List<FacetField> facetFields =
                uniProtEntryService.getFacets(processedQuery, getFacetParams(entries));

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
            List<FacetField.Count> parentFacetCounts,
            String query);

    protected GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            Map<String, T> idEntryMap,
            List<T> ancestorEntries,
            String parentId,
            List<FacetField.Count> parentFacetCounts,
            String query) {
        List<Group> groups =
                facetCounts.stream()
                        .map(fc -> getGroup(fc, idEntryMap.get(getFacetId(fc)), query))
                        .sorted(Group.SORT_BY_LABEL_IGNORE_CASE)
                        .collect(Collectors.toList());
        List<Ancestor> ancestors =
                ancestorEntries.stream().map(this::getAncestor).collect(Collectors.toList());
        Parent parent = getParentInfo(parentId, parentFacetCounts);

        return new GroupByResult(ancestors, groups, parent);
    }

    private Parent getParentInfo(String parentId, List<FacetField.Count> parentFacetCounts) {
        long count = parentFacetCounts.stream().mapToLong(FacetField.Count::getCount).sum();
        return ParentImpl.builder()
                .label(isTopLevelSearch(parentId) ? null : getLabel(getEntryById(parentId)))
                .count(count)
                .build();
    }

    protected abstract T getEntryById(String id);

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
