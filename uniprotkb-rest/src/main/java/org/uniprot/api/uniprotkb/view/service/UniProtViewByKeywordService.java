package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.cv.keyword.KeywordRepo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByKeywordService extends UniProtViewByService<KeywordEntry> {
    public static final String URL_PREFIX = "https://www.uniprot.org/keywords/";
    private final KeywordRepo keywordRepo;

    public UniProtViewByKeywordService(SolrClient solrClient, String uniProtCollection, KeywordRepo keywordRepo) {
        super(solrClient, uniProtCollection);
        this.keywordRepo = keywordRepo;
    }

    @Override
    List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<KeywordEntry> entries, String queryStr) {
        Map<String, KeywordEntry> keywordIdMap = entries.stream()
                .collect(Collectors.toMap(KeywordEntry::getAccession, Function.identity()));
        return facetCounts.stream()
                .map(fc -> createViewBy(fc, keywordIdMap.get(fc.getName()), queryStr))
                .sorted(ViewBy.SORT_BY_LABEL)
                .collect(Collectors.toList());
    }

    private ViewBy createViewBy(FacetField.Count count, KeywordEntry keywordEntry, String queryString) {
        ViewBy viewBy = new ViewBy();
        viewBy.setId(count.getName());
        viewBy.setCount(count.getCount());
        viewBy.setLink(URL_PREFIX + count.getName());
        viewBy.setLabel(keywordEntry.getKeyword().getName());
        viewBy.setExpand(isExpandable(count, queryString));
        return viewBy;
    }

    @Override
    SolrQuery getSolrQuery(String queryStr, List<KeywordEntry> entries) {
        SolrQuery query = new SolrQuery(queryStr);
        String facetItems = entries.stream().map(KeywordEntry::getAccession)
                .collect(Collectors.joining(","));
        String facetField = "{!terms='" + facetItems + "'}keyword_id";
        query.setFacet(true);
        query.addFacetField(facetField);
        return query;
    }

    @Override
    List<KeywordEntry> getChildren(String parent) {
        if (isOpenParentSearch(parent)) {
            return keywordRepo.getAllCategories();
        }
        KeywordEntry keyword = keywordRepo.getByAccession(parent);
        return keyword != null ? keyword.getChildren() : keywordRepo.getAllCategories();
    }
}
