package org.uniprot.api.uniprotkb.groupby.service;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.keyword.KeywordStreamRequest;
import org.uniprot.api.rest.service.keyword.KeywordService;
import org.uniprot.api.uniprotkb.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.cv.keyword.KeywordEntry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UniProtKBGroupByKeywordService extends UniProtKBGroupByService<KeywordEntry> {
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] ";
    private final KeywordService keywordService;

    public UniProtKBGroupByKeywordService(
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
    protected GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<KeywordEntry> keywordEntries,
            List<KeywordEntry> ancestorEntries,
            String query) {
        Map<String, KeywordEntry> idEntryMap =
                keywordEntries.stream().collect(Collectors.toMap(this::getId, Function.identity()));
        return getGroupByResult(facetCounts, idEntryMap, ancestorEntries, query);
    }

    @Override
    protected String getId(KeywordEntry entry) {
        return entry.getAccession();
    }

    @Override
    protected String getLabel(KeywordEntry entry) {
        return entry.getKeyword().getName();
    }
}
