package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.keyword.KeywordStreamRequest;
import org.uniprot.api.rest.service.keyword.KeywordService;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByImpl;
import org.uniprot.core.cv.keyword.KeywordEntry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UniProtViewByKeywordService extends UniProtViewByService<KeywordEntry> {
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] ";
    public static final String URL_PHRASE = "/keywords/";
    private final KeywordService keywordService;

    public UniProtViewByKeywordService(
            KeywordService keywordService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.keywordService = keywordService;
    }

    @Override
    protected List<KeywordEntry> getChildren(String parent) {
        KeywordStreamRequest taxonomyStreamRequest = new KeywordStreamRequest();
        taxonomyStreamRequest.setQuery(isTopLevelSearch(parent) ? TOP_LEVEL_PARENT_QUERY : "parent:" + parent);
        return keywordService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }

    @Override
    protected Map<String, String> getFacetFields(List<KeywordEntry> entries) {
        String facetItems = entries.stream().map(KeywordEntry::getAccession)
                .collect(Collectors.joining(","));
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}keyword", facetItems));
    }

    @Override
    protected List<ViewBy> getViewBys(List<FacetField.Count> facetCounts, List<KeywordEntry> entries, String query) {
        Map<String, KeywordEntry> keywordIdMap = entries.stream()
                .collect(Collectors.toMap(KeywordEntry::getAccession, Function.identity()));
        return facetCounts.stream()
                .map(fc -> getViewBy(fc, keywordIdMap.get(fc.getName()), query))
                .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                .collect(Collectors.toList());
    }

    private ViewBy getViewBy(FacetField.Count count, KeywordEntry keywordEntry, String queryString) {
        return ViewByImpl.builder().id(count.getName())
                .count(count.getCount())
                .link(URL_PHRASE + count.getName())
                .label(keywordEntry.getKeyword().getName())
                .expand(hasChildren(count, queryString)).build();
    }
}
