package org.uniprot.api.uniprotkb.common.service.groupby;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.common.service.ec.ECService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.cv.ec.ECEntry;

@Service
public class GroupByECService extends GroupByService<String> {
    public static final String REGEX_SUFFIX = "[0-9]+";
    public static final String TOKEN_REGEX = "\\.";
    public static final String DASH = ".-";
    public static final String EC = "ec";
    public static final String FACET_MIN_COUNT = "1";
    public static final int MAX_TOKEN_COUNT = 4;
    public static final String TOKEN = ".";
    private final ECService ecService;

    public GroupByECService(ECService ecService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.ecService = ecService;
    }

    @Override
    List<String> getInitialEntries(String parentId) {
        if (!isTopLevelSearch(parentId)) {
            String shortFormParent = getShortFormEc(parentId);
            return shortFormParent.contains(TOKEN)
                    ? List.of(shortFormParent.split(TOKEN_REGEX)[0])
                    : List.of();
        }
        return getChildEntries(parentId);
    }

    @Override
    List<FacetField.Count> getInitialFacetCounts(
            String parentId, String query, List<String> entries) {
        if (isTopLevelSearch(parentId)) {
            return getFacetCounts(query, entries);
        }
        String shortFormParent = getShortFormEc(parentId);
        List<FacetField.Count> facetCounts = getFacetCounts(query, entries);
        return facetCounts.stream()
                .filter(facetCount -> shortFormParent.equals(facetCount.getName()))
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getChildEntries(String parent) {
        List<String> children = new LinkedList<>();
        String parentEC = parent;
        if (!isTopLevelSearch(parentEC)) {
            parentEC = getShortFormEc(parentEC);
            String[] tokens = parentEC.split(TOKEN_REGEX);
            children.addAll(Arrays.asList(tokens));
        }
        return children;
    }

    @Override
    protected Map<String, String> getFacetParams(List<String> entries) {
        String regEx =
                entries.stream()
                        .map(token -> token + TOKEN_REGEX)
                        .collect(Collectors.joining("", "", REGEX_SUFFIX));
        return Map.of(
                FacetParams.FACET_MATCHES,
                regEx,
                FacetParams.FACET_FIELD,
                EC,
                FacetParams.FACET_MINCOUNT,
                FACET_MIN_COUNT);
    }

    @Override
    protected String getId(String entry) {
        return getFullEc(entry);
    }

    @Override
    protected String getLabel(String fullEc) {
        return ecService.getEC(fullEc).map(ECEntry::getLabel).orElse("");
    }

    @Override
    protected void addToAncestors(
            List<String> ancestors,
            List<String> entries,
            String parent,
            List<FacetField.Count> facetCounts) {
        if (!facetCounts.isEmpty()) {
            String facetId = getFacetId(facetCounts.get(0));
            if (!Objects.equals(getShortFormEc(parent), facetId)) {
                ancestors.add(getFullEc(facetId));
            }
        }
    }

    @Override
    protected GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<String> ecs,
            List<String> ancestorEntries,
            String parentId,
            List<FacetField.Count> parentFacetCounts,
            String query) {
        Map<String, String> idEntryMap =
                facetCounts.stream()
                        .collect(
                                Collectors.toMap(
                                        FacetField.Count::getName,
                                        count -> this.getFullEc(count.getName())));
        return getGroupByResult(
                facetCounts, idEntryMap, ancestorEntries, parentId, parentFacetCounts, query);
    }

    @Override
    protected String getEntryById(String id) {
        return id;
    }

    private String getShortFormEc(String fullEc) {
        String temp = fullEc;
        while (StringUtils.isNotEmpty(temp) && temp.endsWith(DASH)) {
            temp = temp.substring(0, temp.length() - 2);
        }
        return temp;
    }

    private String getFullEc(String ec) {
        String[] tokens = ec.split(TOKEN_REGEX);
        int count = MAX_TOKEN_COUNT - tokens.length;
        return ec + DASH.repeat(count);
    }
}
