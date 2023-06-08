package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.keyword.KeywordStreamRequest;
import org.uniprot.api.rest.service.keyword.KeywordService;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.api.uniprotkb.view.Ancestor;
import org.uniprot.api.uniprotkb.view.AncestorImpl;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByImpl;
import org.uniprot.core.cv.keyword.KeywordEntry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UniProtKBViewByKeywordService extends UniProtKBViewByService<KeywordEntry> {
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] ";
    private final KeywordService keywordService;

    public UniProtKBViewByKeywordService(
            KeywordService keywordService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.keywordService = keywordService;
    }

    @Override
    protected List<KeywordEntry> getChildren(String parent) {
        KeywordStreamRequest taxonomyStreamRequest = new KeywordStreamRequest();
        taxonomyStreamRequest.setQuery(
                isTopLevelSearch(parent) ? TOP_LEVEL_PARENT_QUERY : "parent:" + parent);
        return keywordService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }

    @Override
    protected Map<String, String> getFacetFields(List<KeywordEntry> entries) {
        String keywordIds =
                entries.stream().map(KeywordEntry::getAccession).collect(Collectors.joining(","));
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}keyword", keywordIds));
    }

    @Override
    protected String getId(KeywordEntry entry) {
        return entry.getAccession();
    }

    @Override
    protected String getLabel(KeywordEntry entry) {
        return entry.getKeyword().getName();
    }

    /*@Override
    protected ViewByResult getViewBys(
            List<FacetField.Count> facetCounts, List<KeywordEntry> entries, List<KeywordEntry> ancestors, String query) {
        Map<String, KeywordEntry> keywordIdMap =
                entries.stream()
                        .collect(Collectors.toMap(KeywordEntry::getAccession, Function.identity()));
        return new ViewByResult(getAncestors(ancestors), facetCounts.stream()
                .map(fc -> getViewBy(fc, keywordIdMap.get(fc.getName()), query))
                .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                .collect(Collectors.toList()));
    }*/

    private List<Ancestor> getAncestors(List<KeywordEntry> keywords) {
        return keywords.stream()
                .map(kw -> AncestorImpl.builder().id(kw.getAccession()).label(kw.getKeyword().getName()).build())
                .collect(Collectors.toList());
    }

    private ViewBy getViewBy(
            FacetField.Count count, KeywordEntry keywordEntry, String queryString) {
        return ViewByImpl.builder()
                .id(count.getName())
                .label(keywordEntry.getKeyword().getName())
                .count(count.getCount())
                .expand(hasChildren(count, queryString))
                .build();
    }
}
