package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByTaxonomyService implements UniProtViewByService {
    public static final String DEFAULT_PARENT_ID = "1";
    private final SolrClient solrClient;
    private final String uniProtCollection;
    private final TaxonomyQueryService taxonomyQueryService;
    public static final String URL_PREFIX = "https://www.uniprot.org/taxonomy/";

    public UniProtViewByTaxonomyService(
            SolrClient solrClient, String uniProtCollection, TaxonomyQueryService taxonomyQueryService) {
        this.solrClient = solrClient;
        this.uniProtCollection = uniProtCollection;
        this.taxonomyQueryService = taxonomyQueryService;
    }

    @Override
    public List<ViewBy> get(String queryStr, String parent) {
        List<FacetField.Count> facetCounts = Collections.emptyList();
        List<TaxonomyEntry> taxonomyEntries = Collections.emptyList();
        String taxonomyId = isOpenParentSearch(parent) ? DEFAULT_PARENT_ID : parent;

        do {
            List<TaxonomyEntry> childTaxonomyEntries = getChildren(taxonomyId);
            List<FacetField.Count> childFacetCounts = getFacetCounts(queryStr, childTaxonomyEntries);

            if (!childFacetCounts.isEmpty()) {
                facetCounts = childFacetCounts;
                taxonomyEntries = childTaxonomyEntries;
                taxonomyId = facetCounts.get(0).getName();
            } else {
                break;
            }

        } while (facetCounts.size() == 1 && isOpenParentSearch(parent));

        return createViewBys(facetCounts, taxonomyEntries, queryStr);
    }

    private static boolean isOpenParentSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    private List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<TaxonomyEntry> taxonomyEntries, String queryStr) {
        List<ViewBy> viewBys = Collections.emptyList();

        if (!facetCounts.isEmpty()) {
            Map<Long, TaxonomyEntry> taxIdMap = taxonomyEntries.stream().collect(Collectors.toMap(Taxonomy::getTaxonId, Function.identity()));
            viewBys = facetCounts.stream()
                    .map(fc -> createViewBy(fc, taxIdMap.get(Long.parseLong(fc.getName())), queryStr))
                    .sorted(ViewBy.SORT_BY_LABEL)
                    .collect(Collectors.toList());
        }

        return viewBys;
    }

    private ViewBy createViewBy(FacetField.Count count, TaxonomyEntry taxonomyEntry, String queryStr) {
        ViewBy viewBy = new ViewBy();
        viewBy.setId(count.getName());
        viewBy.setCount(count.getCount());
        viewBy.setLink(URL_PREFIX + count.getName());
        viewBy.setLabel(taxonomyEntry.getScientificName());
        viewBy.setExpand(hasChildren(count, queryStr));
        return viewBy;
    }

    private boolean hasChildren(FacetField.Count count, String queryStr) {
        return !getFacetCounts(queryStr, getChildren(count.getName())).isEmpty();
    }

    private List<FacetField.Count> getFacetCounts(String queryStr, List<TaxonomyEntry> taxonomyEntries) {
        List<FacetField.Count> result = Collections.emptyList();

        if (!taxonomyEntries.isEmpty()) {
            List<FacetField> facetFields = getFacetFields(queryStr, taxonomyEntries);

            if (!facetFields.isEmpty()) {
                return facetFields.get(0).getValues().stream()
                        .filter(count -> count.getCount() > 0).collect(Collectors.toList());
            }
        }

        return result;
    }

    private List<FacetField> getFacetFields(String queryStr, List<TaxonomyEntry> taxonomyEntries) {
        try {
            String facetItems = taxonomyEntries.stream()
                    .map(val -> String.valueOf(val.getTaxonId()))
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

    public List<TaxonomyEntry> getChildren(String taxId) {
        return taxonomyQueryService.getChildren(taxId);
    }
}
