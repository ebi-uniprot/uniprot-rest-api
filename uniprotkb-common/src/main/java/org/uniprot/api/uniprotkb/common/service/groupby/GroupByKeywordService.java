package org.uniprot.api.uniprotkb.common.service.groupby;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.common.keyword.request.KeywordStreamRequest;
import org.uniprot.api.support.data.common.keyword.service.KeywordService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.cv.keyword.KeywordEntry;

@Service
public class GroupByKeywordService extends GroupByService<KeywordEntry> {
    public static final String TOP_LEVEL_KEYWORD_PARENT_QUERY = "-parent:[* TO *] ";
    private final KeywordService keywordService;

    public GroupByKeywordService(
            KeywordService keywordService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.keywordService = keywordService;
    }

    @Override
    protected List<KeywordEntry> getChildEntries(String parent) {
        KeywordStreamRequest taxonomyStreamRequest = new KeywordStreamRequest();
        taxonomyStreamRequest.setQuery(
                isTopLevelSearch(parent) ? TOP_LEVEL_KEYWORD_PARENT_QUERY : "parent:" + parent);
        return keywordService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }

    @Override
    protected Map<String, String> getFacetParams(List<KeywordEntry> entries) {
        String keywordIds =
                entries.stream()
                        .map(KeywordEntry::getAccession)
                        .map(String::toLowerCase)
                        .collect(Collectors.joining(","));
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}keyword", keywordIds));
    }

    @Override
    protected GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<KeywordEntry> keywordEntries,
            List<KeywordEntry> ancestorEntries,
            String parentId,
            List<FacetField.Count> parentFacetCounts,
            String query) {
        Map<String, KeywordEntry> idEntryMap =
                keywordEntries.stream().collect(Collectors.toMap(this::getId, Function.identity()));
        return getGroupByResult(
                facetCounts, idEntryMap, ancestorEntries, parentId, parentFacetCounts, query);
    }

    @Override
    protected List<FacetField.Count> getFacetCounts(String query, List<KeywordEntry> entries) {
        List<FacetField.Count> result = super.getFacetCounts(query, entries);
        return result.stream()
                .map(
                        c -> {
                            c.setName(c.getName().toUpperCase());
                            return c;
                        })
                .collect(Collectors.toList());
    }

    @Override
    protected KeywordEntry getEntryById(String id) {
        return keywordService.findByUniqueId(id);
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
