package org.uniprot.api.common.repository.search.facet;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.json.BucketBasedJsonFacet;
import org.apache.solr.client.solrj.response.json.BucketJsonFacet;
import org.apache.solr.client.solrj.response.json.NestableJsonFacet;
import org.uniprot.core.util.Utils;

/**
 * This interface is responsible to convert QueryResponse facets to a List<Facet> response model.
 *
 * <p>During the conversion it also add configuration label/properties from facet.property
 *
 * @author lgonzales
 */
public class FacetResponseConverter extends FacetConverter<QueryResponse, List<Facet>> {

    private final FacetConfig facetConfig;

    public FacetResponseConverter(FacetConfig facetConfig) {
        this.facetConfig = facetConfig;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return this.facetConfig;
    }

    /**
     * This method is responsible to convert QueryResponse facets to a List<Facet> response model,
     * adding configured labels and properties
     *
     * @param queryResponse Solr query Response
     * @return List of Facet converted and configured.
     */
    @Override
    public List<Facet> convert(QueryResponse queryResponse, List<String> facetList) {
        List<Facet> facetResult = new ArrayList<>();

        if (Utils.notNull(queryResponse.getJsonFacetingResponse())) {
            NestableJsonFacet facetResponse = queryResponse.getJsonFacetingResponse();
            for (String facetName : facetList) {
                // Iterating over all requested Facets
                BucketBasedJsonFacet facetField = facetResponse.getBucketBasedFacets(facetName);
                if (facetField != null) {
                    if (isIntervalFacet(facetName)) {
                        facetResult.add(convertIntervalFacets(facetField, facetName));
                    } else {
                        facetResult.add(convertFieldFacets(facetField, facetName));
                    }
                }
            }
        }

        return facetResult;
    }

    /**
     * This method is responsible to convert Solr Interval facet to a Facet response model, adding
     * configured labels and properties
     *
     * @param intervalFacet interval facet returned from Solr
     * @return converted facet
     */
    private Facet convertIntervalFacets(BucketBasedJsonFacet intervalFacet, String facetName) {
        // Iterating over all Query response Interval Facets
        List<FacetItem> values = new ArrayList<>();
        if (Utils.notNullNotEmpty(intervalFacet.getBuckets())) {
            for (BucketJsonFacet count : intervalFacet.getBuckets()) {
                // Iterating over all query response interval facet items
                if (count != null && count.getCount() > 0) {
                    String value = count.getVal().toString();
                    String queryTerm = value.replace(",", " TO ");
                    // Adding add Facet Item to facet item list
                    values.add(
                            FacetItem.builder()
                                    .value(queryTerm)
                                    .label(getIntervalFacetItemLabel(facetName, value))
                                    .count(count.getCount())
                                    .build());
                }
            }
        }
        // return an Interval facet
        return Facet.builder()
                .name(facetName)
                .label(getFacetLabel(facetName))
                .allowMultipleSelection(allowMultipleSelection(facetName))
                .values(values)
                .build();
    }

    /**
     * This method is responsible to convert Solr Field facet to a Facet response model, adding
     * configured labels and properties
     *
     * @param facetField Facet Field returned from Solr
     * @return converted field facet
     */
    private Facet convertFieldFacets(BucketBasedJsonFacet facetField, String facetName) {
        List<FacetItem> values = new ArrayList<>();
        if (Utils.notNullNotEmpty(facetField.getBuckets())) {
            for (BucketJsonFacet count : facetField.getBuckets()) {
                // Iterating over all query response facet items
                if (count != null && count.getCount() > 0) {
                    // Adding add Facet Item to facet item list
                    String itemValue = count.getVal().toString();
                    values.add(
                            FacetItem.builder()
                                    .value(itemValue)
                                    .label(getFacetItemLabel(facetName, itemValue))
                                    .count(count.getCount())
                                    .build());
                }
            }
        }
        // build a facet
        return Facet.builder()
                .name(facetName)
                .label(getFacetLabel(facetName))
                .allowMultipleSelection(allowMultipleSelection(facetName))
                .values(values)
                .build();
    }
}
