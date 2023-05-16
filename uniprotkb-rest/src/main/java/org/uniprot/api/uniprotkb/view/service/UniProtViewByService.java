package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class UniProtViewByService<T> {

    private final SolrClient solrClient;
    private final String uniProtCollection;

    protected UniProtViewByService(SolrClient solrClient, String uniProtCollection) {
        this.solrClient = solrClient;
        this.uniProtCollection = uniProtCollection;
    }

    public List<ViewBy> get(String queryStr, String parent) {
        List<FacetField.Count> facetCounts = Collections.emptyList();
        List<T> entries = Collections.emptyList();
        String id = parent;

        do {
            List<T> children = getChildren(id);
            List<FacetField.Count> childFacetCounts = getFacetCounts(queryStr, children);

            if (!childFacetCounts.isEmpty()) {
                facetCounts = childFacetCounts;
                entries = children;
                id = facetCounts.get(0).getName();
            } else {
                break;
            }

        } while (facetCounts.size() == 1 && isOpenParentSearch(parent));

        return createViewBys(facetCounts, entries, queryStr);
    }


    private List<FacetField.Count> getFacetCounts(String queryStr, List<T> entries) {
        List<FacetField.Count> result = Collections.emptyList();

        if (!entries.isEmpty()) {
            List<FacetField> facetFields = getFacetFields(getSolrQuery(queryStr, entries));

            if (!facetFields.isEmpty()) {
                return facetFields.get(0).getValues().stream()
                        .filter(count -> count.getCount() > 0).collect(Collectors.toList());
            }
        }

        return result;
    }


    private List<FacetField> getFacetFields(SolrQuery query) {
        try {
            QueryResponse response = solrClient.query(uniProtCollection, query);
            return response.getFacetFields();
        } catch (Exception e) {
            throw new UniProtViewByServiceException(e);
        }
    }

    protected boolean isExpandable(FacetField.Count count, String queryStr) {
        return !getFacetCounts(queryStr, getChildren(count.getName())).isEmpty();
    }

    protected boolean isOpenParentSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    abstract List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<T> entries, String queryStr);

    abstract SolrQuery getSolrQuery(String queryStr, List<T> entries);

    abstract List<T> getChildren(String parent);
}
