package org.uniprot.api.uniprotkb.view.service;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.cv.ec.ECEntry;

@Service
public class UniProtKBViewByECService extends UniProtKBViewByService<String> {
    public static final String REGEX_SUFFIX = "[0-9]+";
    public static final String TOKEN_REGEX = "\\.";
    public static final String DASH = ".-";
    public static final String EC = "ec";
    public static final String FACET_MIN_COUNT = "1";
    public static final int MAX_TOKEN_COUNT = 4;
    private final ECService ecService;

    public UniProtKBViewByECService(ECService ecService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.ecService = ecService;
    }

    @Override
    protected List<String> getChildren(String parent) {
        List<String> children = new LinkedList<>();
        String parentEc = parent;
        if (!isTopLevelSearch(parentEc)) {
            parentEc = getShortFormEc(parentEc);
            String[] tokens = parentEc.split(TOKEN_REGEX);
            children.addAll(Arrays.asList(tokens));
        }
        return children;
    }

    @Override
    protected Map<String, String> getFacetFields(List<String> entries) {
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
    protected Map<String, String> getEntryMap(
            List<String> entries, List<FacetField.Count> facetCounts) {
        return facetCounts.stream()
                .collect(
                        Collectors.toMap(
                                FacetField.Count::getName,
                                count -> this.getFullEc(count.getName())));
    }

    @Override
    protected void addToAncestors(
            List<String> ancestors, List<String> entries, String parent, String id) {
        if (!Objects.equals(parent, id)) {
            ancestors.add(getFullEc(id));
        }
    }

    private String getShortFormEc(String fullEc) {
        String temp = fullEc;
        while (temp.endsWith(DASH)) {
            temp = temp.substring(0, temp.length() - 2);
        }
        return temp;
    }

    private String getFullEc(String ec) {
        String[] tokens = ec.split(TOKEN_REGEX);
        int count = MAX_TOKEN_COUNT - tokens.length;
        return ec + DASH.repeat(Math.max(0, count));
    }
}
