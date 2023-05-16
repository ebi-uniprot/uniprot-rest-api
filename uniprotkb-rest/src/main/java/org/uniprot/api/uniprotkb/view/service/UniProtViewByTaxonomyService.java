package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.uniprotkb.view.TaxonomyNode;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByTaxonomyService extends UniProtViewByService<TaxonomyNode> {
    public static final String URL_PREFIX = "https://www.uniprot.org/taxonomy/";
    private final TaxonomyService taxonomyService;

    public UniProtViewByTaxonomyService(SolrClient solrClient, String uniProtCollection, TaxonomyService taxonomyService) {
        super(solrClient, uniProtCollection);
        this.taxonomyService = taxonomyService;
    }

    @Override
    public List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<TaxonomyNode> entries, String queryStr) {
            Map<Long, TaxonomyNode> taxIdMap = entries.stream()
                    .collect(Collectors.toMap(TaxonomyNode::getTaxonomyId, Function.identity()));
            return facetCounts.stream()
                    .map(fc -> createViewBy(fc, taxIdMap.get(Long.parseLong(fc.getName())), queryStr))
                    .sorted(ViewBy.SORT_BY_LABEL)
                    .collect(Collectors.toList());
    }

    private ViewBy createViewBy(FacetField.Count count, TaxonomyNode taxonomyNode, String queryStr) {
        ViewBy viewBy = new ViewBy();
        viewBy.setId(count.getName());
        viewBy.setCount(count.getCount());
        viewBy.setLink(URL_PREFIX + count.getName());
        viewBy.setLabel(taxonomyNode.getFullName());
        viewBy.setExpand(isExpandable(count, queryStr));
        return viewBy;
    }

    @Override
    public SolrQuery getSolrQuery(String queryStr, List<TaxonomyNode> entries) {
        SolrQuery query = new SolrQuery(queryStr);
        String facetItems = entries.stream()
                .map(val -> String.valueOf(val.getTaxonomyId()))
                .collect(Collectors.joining(","));
        String facetField = "{!terms='" + facetItems + "'}taxonomy_id";
        query.setFacet(true);
        query.addFacetField(facetField);
        return query;
    }

    @Override
    public List<TaxonomyNode> getChildren(String parent) {
        String taxonomyId = isOpenParentSearch(parent) ? "1" : parent;
        return taxonomyService.getChildren(taxonomyId);
    }
}
