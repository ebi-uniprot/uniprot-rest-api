package org.uniprot.api.common.repository.search.facet;

import java.util.*;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetStreamExpression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Class to keep the response returned by List of facet value search and facet functions.
 *
 * @author sahmad
 * @created 29/07/2020
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SolrStreamFacetResponse {
    private List<Facet> facets;
    private List<String> ids;

    public static class Builder {
        private List<Facet> facets = new ArrayList<>();
        private List<String> ids = new ArrayList<>();

        public Builder facets(List<Facet> newFacets) {
            if (newFacets != null) {
                this.facets.addAll(newFacets);
            }
            return this;
        }

        public Builder ids(List<String> newIds) {
            if (newIds != null) {
                this.ids.addAll(newIds);
            }
            return this;
        }

        public SolrStreamFacetResponse build() {
            return new SolrStreamFacetResponse(facets, ids);
        }
    }

    public static SolrStreamFacetResponse merge(
            SolrRequest solrRequest,
            List<SolrStreamFacetResponse> responses,
            SolrStreamFacetResponse idsResponse) {
        List<Facet> mergedFacets = mergeBatchFacets(solrRequest, responses);
        Builder builder = new Builder();
        builder.facets(mergedFacets);
        builder.ids(idsResponse.getIds());
        return builder.build();
    }

    private static List<Facet> mergeBatchFacets(
            SolrRequest solrRequest, List<SolrStreamFacetResponse> facetsInBatches) {
        Map<String, Map.Entry<Integer, Comparator<FacetItem>>> map =
                FacetStreamExpression.getFacetNameComparatorAndLimitMap(solrRequest.getFacets());
        List<List<Facet>> facetsLists =
                facetsInBatches.stream().map(SolrStreamFacetResponse::getFacets).toList();
        Map<String, Facet.FacetBuilder> mergedFacetsMap = getFacetBuilderMap(facetsLists);
        List<Facet> mergedFacets = getMergedFacets(mergedFacetsMap, map);
        return mergedFacets;
    }

    private static Map<String, Facet.FacetBuilder> getFacetBuilderMap(
            List<List<Facet>> facetsLists) {
        Map<String, Facet.FacetBuilder> mergedFacetsMap = new LinkedHashMap<>();
        for (List<Facet> facets : facetsLists) {
            for (Facet facet : facets) {
                String facetLabel = facet.getLabel();
                // If a Facet with the same label already exists, merge its FacetItems
                if (mergedFacetsMap.containsKey(facetLabel)) {
                    Facet.FacetBuilder existingBuilder = mergedFacetsMap.get(facetLabel);
                    mergeFacetItems(existingBuilder, facet);
                } else {
                    // Otherwise, add it to the mergedFacetsMap
                    Facet.FacetBuilder newBuilder =
                            Facet.builder()
                                    .label(facet.getLabel())
                                    .name(facet.getName())
                                    .allowMultipleSelection(facet.isAllowMultipleSelection());
                    mergeFacetItems(newBuilder, facet);
                    mergedFacetsMap.put(facetLabel, newBuilder);
                }
            }
        }
        return mergedFacetsMap;
    }

    private static List<Facet> getMergedFacets(
            Map<String, Facet.FacetBuilder> mergedFacetsMap,
            Map<String, Map.Entry<Integer, Comparator<FacetItem>>> map) {
        // Build the final list of Facets from the builders
        List<Facet> mergedFacets = new ArrayList<>();
        for (Facet.FacetBuilder builder : mergedFacetsMap.values()) {
            Facet mergedFacet = builder.build();
            Map.Entry<Integer, Comparator<FacetItem>> limitComparator =
                    map.get(mergedFacet.getName());
            List<FacetItem> facetItems = mergedFacet.getValues();
            facetItems.sort(limitComparator.getValue());
            Integer limit = limitComparator.getKey();
            builder.values(facetItems.subList(0, Math.min(facetItems.size(), limit)));
            mergedFacets.add(builder.build());
        }
        return mergedFacets;
    }

    private static void mergeFacetItems(Facet.FacetBuilder builder, Facet sourceFacet) {
        // Initialize itemMap to store merged FacetItems
        Map<String, FacetItem.FacetItemBuilder> itemMap = new LinkedHashMap<>();

        // Collect existing FacetItems from the builder (handling potential null)
        List<FacetItem> existingValues = builder.build().getValues();
        if (existingValues != null) {
            for (FacetItem existingItem : existingValues) {
                String key = existingItem.getLabel() + "|" + existingItem.getValue();
                FacetItem.FacetItemBuilder itemBuilder = getFacetItemBuilder(existingItem);
                itemMap.put(key, itemBuilder);
            }
        }

        // Merge FacetItems from the sourceFacet (handling potential null in
        // sourceFacet.getValues())
        List<FacetItem> sourceValues = sourceFacet.getValues();
        if (sourceValues != null) {
            for (FacetItem facetItem : sourceValues) {
                String key = facetItem.getLabel() + "|" + facetItem.getValue();
                if (itemMap.containsKey(key)) {
                    // Add counts if the item already exists
                    FacetItem.FacetItemBuilder existingBuilder = itemMap.get(key);
                    existingBuilder.count(
                            existingBuilder.build().getCount() + facetItem.getCount());
                } else {
                    // Otherwise, add the new item
                    FacetItem.FacetItemBuilder newItemBuilder = getFacetItemBuilder(facetItem);
                    itemMap.put(key, newItemBuilder);
                }
            }
        }

        // Update the builder's values with the merged items
        List<FacetItem> facetItems =
                new ArrayList<>(); // Ensure the builder is initialized with a new list
        for (FacetItem.FacetItemBuilder itemBuilder : itemMap.values()) {
            facetItems.add(itemBuilder.build());
        }
        builder.values(facetItems);
    }

    private static FacetItem.FacetItemBuilder getFacetItemBuilder(FacetItem existingItem) {
        FacetItem.FacetItemBuilder itemBuilder =
                FacetItem.builder()
                        .label(existingItem.getLabel())
                        .value(existingItem.getValue())
                        .count(existingItem.getCount());
        return itemBuilder;
    }
}
