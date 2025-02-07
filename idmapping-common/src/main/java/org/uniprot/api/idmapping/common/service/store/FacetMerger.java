package org.uniprot.api.idmapping.common.service.store;

import java.util.*;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.solrstream.FacetStreamExpression;

public class FacetMerger {

    public static List<Facet> mergeBatchFacets(SolrRequest solrRequest, List<SolrStreamFacetResponse> facetsInBatches) {
        Map<String, Map.Entry<Integer, Comparator<FacetItem>>> map = FacetStreamExpression.getFacetNameComparatorAndLimitMap(solrRequest.getFacets());
        List<List<Facet>> facetsLists =
                facetsInBatches.stream().map(SolrStreamFacetResponse::getFacets).toList();
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

        // Build the final list of Facets from the builders
        List<Facet> mergedFacets = new ArrayList<>();
        for (Facet.FacetBuilder builder : mergedFacetsMap.values()) {
            Facet mergedFacet = builder.build();
            Map.Entry<Integer, Comparator<FacetItem>> limitComparator = map.get(mergedFacet.getName());
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
                FacetItem.FacetItemBuilder itemBuilder =
                        FacetItem.builder()
                                .label(existingItem.getLabel())
                                .value(existingItem.getValue())
                                .count(existingItem.getCount());
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
                    FacetItem.FacetItemBuilder newItemBuilder =
                            FacetItem.builder()
                                    .label(facetItem.getLabel())
                                    .value(facetItem.getValue())
                                    .count(facetItem.getCount());
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
}
