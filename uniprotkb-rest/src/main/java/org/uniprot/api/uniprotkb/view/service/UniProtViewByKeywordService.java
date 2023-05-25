package org.uniprot.api.uniprotkb.view.service;

import com.google.common.base.Strings;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByImpl;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.cv.keyword.KeywordRepo;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByKeywordService extends UniProtViewByService<KeywordEntry> {
    private final SolrClient solrClient;
    private final String uniprotCollection;
    private final KeywordRepo keywordRepo;
    public static final String URL_PREFIX = "https://www.uniprot.org/keywords/";

    public UniProtViewByKeywordService(
            SolrClient solrClient, String uniprotCollection, KeywordRepo keywordRepo) {
        this.solrClient = solrClient;
        this.uniprotCollection = uniprotCollection;
        this.keywordRepo = keywordRepo;
    }

    @Override
    public List<ViewBy> getViewBys(String queryStr, String parent) {
        List<KeywordEntry> keywords = getKeywordsFromParent(parent);
        if (keywords.isEmpty()) return Collections.emptyList();
        Map<String, KeywordEntry> keywordAccMap =
                keywords.stream()
                        .collect(Collectors.toMap(KeywordEntry::getAccession, Function.identity()));
        String facetIterms =
                keywords.stream().map(val -> val.getAccession()).collect(Collectors.joining(","));
        SolrQuery query = new SolrQuery(queryStr);
        String facetField = "{!terms='" + facetIterms + "'}keyword_id";
        query.setFacet(true);
        query.addFacetField(facetField);

        try {
            QueryResponse response = solrClient.query(uniprotCollection, query);
            List<FacetField> fflist = response.getFacetFields();
            if (fflist.isEmpty()) {
                return Collections.emptyList();
            } else {
                FacetField ff = fflist.get(0);
                List<FacetField.Count> counts = ff.getValues();
                return counts.stream()
                        .map(val -> convert(val, keywordAccMap))
                        .filter(val -> val != null)
                        .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                        .collect(Collectors.toList());
            }
        } catch (SolrServerException | IOException e) {
            throw new UniProtViewByServiceException(e);
        }
    }

    @Override
    protected List<KeywordEntry> getChildren(String parent) {
        if (isTopLevelSearch(parent)) {
            return keywordRepo.getAllCategories();
        }
        KeywordEntry keyword = keywordRepo.getByAccession(parent);
        return keyword != null ? keyword.getChildren() : keywordRepo.getAllCategories();
    }

    @Override
    protected String getFacetFields(List<KeywordEntry> keywordEntries) {
        String facetItems = keywordEntries.stream().map(KeywordEntry::getAccession)
                .collect(Collectors.joining(","));
        return String.format("{!terms='%s'}keyword_id", facetItems);
    }

    @Override
    protected List<ViewBy> getViewBys(List<FacetField.Count> facetCounts, List<KeywordEntry> keywordEntries, String query) {
        return null;
    }

    private ViewBy convert(FacetField.Count count, Map<String, KeywordEntry> keywordAccMap) {
        Optional<KeywordEntry> keyword = Optional.ofNullable(keywordAccMap.get(count.getName()));
        return ViewByImpl.builder()
                .id(count.getName())
                .count(count.getCount())
                .link(URL_PREFIX + count.getName())
                .label(keyword.map(keywordEntry -> keywordEntry.getKeyword().getName()).orElse(""))
                .expand(keyword.map(keywordEntry -> !keywordEntry.getChildren().isEmpty()).orElse(false))
                .build();
    }

    private List<KeywordEntry> getKeywordsFromParent(String parent) {
        if (Strings.isNullOrEmpty(parent)) {
            return keywordRepo.getAllCategories();
        }
        KeywordEntry keyword = keywordRepo.getByAccession(parent);
        if (keyword == null) return keywordRepo.getAllCategories();
        return keyword.getChildren();
    }
}
