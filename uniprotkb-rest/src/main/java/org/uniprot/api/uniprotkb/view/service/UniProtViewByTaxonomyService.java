package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.TaxonomyNode;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByTaxonomyService implements UniProtViewByService {
    private final SolrClient solrClient;
    private final String uniProtCollection;
    private final TaxonomyService taxonomyService;
    public static final String URL_PREFIX = "https://www.uniprot.org/taxonomy/";

    public UniProtViewByTaxonomyService(
            SolrClient solrClient, String uniProtCollection, TaxonomyService taxonomyService) {
        this.solrClient = solrClient;
        this.uniProtCollection = uniProtCollection;
        this.taxonomyService = taxonomyService;
    }

    @Override
    public List<ViewBy> get(String queryStr, String parent) {
        List<FacetField.Count> facetCounts = Collections.emptyList();
        List<TaxonomyNode> taxonomyNodes = Collections.emptyList();
        String taxonomyId = isOpenParentSearch(parent) ? "1" : parent;

        do {
            List<TaxonomyNode> childTaxonomyNodes = getChildren(taxonomyId);
            List<FacetField.Count> childFacetCounts = getFacetCounts(queryStr, childTaxonomyNodes);

            if (!childFacetCounts.isEmpty()) {
                facetCounts = childFacetCounts;
                taxonomyNodes = childTaxonomyNodes;
                taxonomyId = facetCounts.get(0).getName();
            } else {
                break;
            }

        } while (facetCounts.size() == 1 && isOpenParentSearch(parent));

        return createViewBys(facetCounts, taxonomyNodes, queryStr);
    }

    private static boolean isOpenParentSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    private List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<TaxonomyNode> taxonomyNodes, String queryStr) {
        List<ViewBy> viewBys = Collections.emptyList();

        if (!facetCounts.isEmpty()) {
            Map<Long, TaxonomyNode> taxIdMap = taxonomyNodes.stream().collect(Collectors.toMap(TaxonomyNode::getTaxonomyId, Function.identity()));
            viewBys = facetCounts.stream()
                    .map(fc -> createViewBy(fc, taxIdMap.get(Long.parseLong(fc.getName())), queryStr))
                    .sorted(ViewBy.SORT_BY_LABEL)
                    .collect(Collectors.toList());
        }

        return viewBys;
    }

    private ViewBy createViewBy(FacetField.Count count, TaxonomyNode taxonomyNode, String queryStr) {
        ViewBy viewBy = new ViewBy();
        viewBy.setId(count.getName());
        viewBy.setCount(count.getCount());
        viewBy.setLink(URL_PREFIX + count.getName());
        viewBy.setLabel(taxonomyNode.getFullName());
        viewBy.setExpand(hasChildren(count, queryStr));
        return viewBy;
    }

    private boolean hasChildren(FacetField.Count count, String queryStr) {
        return !getFacetCounts(queryStr, getChildren(count.getName())).isEmpty();
    }

    private List<FacetField.Count> getFacetCounts(String queryStr, List<TaxonomyNode> taxonomyNodes) {
        List<FacetField.Count> result = Collections.emptyList();

        if (!taxonomyNodes.isEmpty()) {
            List<FacetField> facetFields = getFacetFields(queryStr, taxonomyNodes);

            if (!facetFields.isEmpty()) {
                return facetFields.get(0).getValues().stream()
                        .filter(count -> count.getCount() > 0).collect(Collectors.toList());
            }
        }

        return result;
    }

    private List<FacetField> getFacetFields(String queryStr, List<TaxonomyNode> taxonomyNodes) {
        try {
            String facetItems = taxonomyNodes.stream()
                    .map(val -> String.valueOf(val.getTaxonomyId()))
                    .collect(Collectors.joining(","));
            SolrQuery query = new SolrQuery(queryStr);
            String facetField = "{!terms='" + facetItems + "'}taxonomy_id";
            query.setFacet(true);
            query.addFacetField(facetField);

            QueryResponse response = solrClient.query(uniProtCollection, query);
            return response.getFacetFields();
        } catch (Exception e) {
            throw new UniProtViewByServiceException(e);
        }
    }

    public List<TaxonomyNode> getChildren(String taxId) {
        return taxonomyService.getChildren(taxId);
    }
}
