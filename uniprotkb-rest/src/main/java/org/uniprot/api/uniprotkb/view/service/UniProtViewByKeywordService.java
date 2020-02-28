package org.uniprot.api.uniprotkb.view.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.cv.keyword.KeywordService;

import com.google.common.base.Strings;

public class UniProtViewByKeywordService implements UniProtViewByService {
    private final SolrClient solrClient;
    private final String uniprotCollection;
    private final KeywordService keywordService;
    public static final String URL_PREFIX = "https://www.uniprot.org/keywords/";

    public UniProtViewByKeywordService(
            SolrClient solrClient, String uniprotCollection, KeywordService keywordService) {
        this.solrClient = solrClient;
        this.uniprotCollection = uniprotCollection;
        this.keywordService = keywordService;
    }

    @Override
    public List<ViewBy> get(String queryStr, String parent) {
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
                        .sorted(ViewBy.SORT_BY_LABEL)
                        .collect(Collectors.toList());
            }
        } catch (SolrServerException | IOException e) {
            throw new UniProtViewByServiceException(e);
        }
    }

    private ViewBy convert(FacetField.Count count, Map<String, KeywordEntry> keywordAccMap) {
        if (count.getCount() == 0) return null;
        ViewBy viewBy = new ViewBy();

        viewBy.setId(count.getName());
        viewBy.setCount(count.getCount());
        KeywordEntry keyword = keywordAccMap.get(count.getName());
        viewBy.setLink(URL_PREFIX + count.getName());
        if (keyword != null) {
            viewBy.setLabel(keyword.getKeyword().getName());
            viewBy.setExpand(!keyword.getChildren().isEmpty());
        }
        return viewBy;
    }

    private List<KeywordEntry> getKeywordsFromParent(String parent) {
        if (Strings.isNullOrEmpty(parent)) {
            return keywordService.getAllCategories();
        }
        KeywordEntry keyword = keywordService.getByAccession(parent);
        if (keyword == null) return keywordService.getAllCategories();
        return keyword.getChildren();
    }
}
