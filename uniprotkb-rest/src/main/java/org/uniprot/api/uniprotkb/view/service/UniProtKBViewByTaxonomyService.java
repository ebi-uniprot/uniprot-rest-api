package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.rest.request.taxonomy.TaxonomyStreamRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtKBViewByTaxonomyService implements UniProtViewByService {
    public static final String DEFAULT_PARENT_ID = "1";
    public static final String TOP_LEVEL_PARENT_QUERY = "-parent:[* TO *] AND active:true";
    public static final String URL_PREFIX = "https://www.uniprot.org/taxonomy/";
    private final SolrClient solrClient;
    private final String uniProtCollection;
    private final TaxonomyService taxonomyService;

    public UniProtKBViewByTaxonomyService(
            SolrClient solrClient,
            String uniProtCollection,
            TaxonomyService taxonomyService) {
        this.solrClient = solrClient;
        this.uniProtCollection = uniProtCollection;
        this.taxonomyService = taxonomyService;
    }

    @Override
    public List<ViewBy> getViewBys(String queryStr, String parent) {
        List<FacetField.Count> facetCounts = List.of();
        List<TaxonomyEntry> taxonomyEntries = List.of();
        String taxonomyId = isTopLevelSearch(parent) ? DEFAULT_PARENT_ID : parent;

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

        } while (facetCounts.size() == 1 && isTopLevelSearch(parent));

        return getViewBys(facetCounts, taxonomyEntries, queryStr);
    }

    private static boolean isTopLevelSearch(String parent) {
        return StringUtils.isEmpty(parent);
    }

    private List<ViewBy> getViewBys(
            List<FacetField.Count> facetCounts,
            List<TaxonomyEntry> taxonomyEntries,
            String queryStr) {
        List<ViewBy> viewBys = List.of();

        if (!facetCounts.isEmpty()) {
            Map<Long, TaxonomyEntry> taxIdMap =
                    taxonomyEntries.stream()
                            .collect(Collectors.toMap(Taxonomy::getTaxonId, Function.identity()));
            viewBys =
                    facetCounts.stream()
                            .map(
                                    fc ->
                                            getViewBy(
                                                    fc,
                                                    taxIdMap.get(Long.parseLong(fc.getName())),
                                                    queryStr))
                            .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                            .collect(Collectors.toList());
        }

        return viewBys;
    }

    private ViewBy getViewBy(
            FacetField.Count count, TaxonomyEntry taxonomyEntry, String queryStr) {
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

    private List<FacetField.Count> getFacetCounts(
            String queryStr, List<TaxonomyEntry> taxonomyEntries) {
        List<FacetField.Count> facetCounts = List.of();

        if (!taxonomyEntries.isEmpty()) {
            List<FacetField> facetFields = getFacetFields(queryStr, taxonomyEntries);

            if (!facetFields.isEmpty()) {
                return facetFields.get(0).getValues().stream()
                        .filter(count -> count.getCount() > 0)
                        .collect(Collectors.toList());
            }
        }

        return facetCounts;
    }

    private List<FacetField> getFacetFields(String queryStr, List<TaxonomyEntry> taxonomyEntries) {
        try {
            String facetItems =
                    taxonomyEntries.stream()
                            .map(taxonomy -> String.valueOf(taxonomy.getTaxonId()))
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
        TaxonomyStreamRequest taxonomyStreamRequest = new TaxonomyStreamRequest();
        taxonomyStreamRequest.setQuery(DEFAULT_PARENT_ID.equals(taxId) ? TOP_LEVEL_PARENT_QUERY : "parent:" + taxId);
        return taxonomyService.stream(taxonomyStreamRequest).collect(Collectors.toList());
    }
}
