package org.uniprot.api.idmapping.common.service.store;

import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;

import java.util.*;

public class FacetMerger {

    public static List<Facet> mergeCollectedFacets(List<List<Facet>> collectedFacets) {
        Map<String, Facet.FacetBuilder> mergedFacetsMap = new HashMap<>();

        for (List<Facet> facetList : collectedFacets) {
            for (Facet incomingFacet : facetList) {
                String facetLabel = incomingFacet.getLabel();

                // If a Facet with the same label already exists, merge its FacetItems
                if (mergedFacetsMap.containsKey(facetLabel)) {
                    Facet.FacetBuilder existingBuilder = mergedFacetsMap.get(facetLabel);
                    mergeFacetItems(existingBuilder, incomingFacet);
                } else {
                    // Otherwise, add it to the mergedFacetsMap
                    Facet.FacetBuilder newBuilder = Facet.builder()
                            .label(incomingFacet.getLabel())
                            .name(incomingFacet.getName())
                            .allowMultipleSelection(incomingFacet.isAllowMultipleSelection());
                    mergeFacetItems(newBuilder, incomingFacet);
                    mergedFacetsMap.put(facetLabel, newBuilder);
                }
            }
        }

        // Build the final list of Facets from the builders
        List<Facet> mergedFacets = new ArrayList<>();
        for (Facet.FacetBuilder builder : mergedFacetsMap.values()) {
            mergedFacets.add(builder.build());
        }
        return mergedFacets;
    }

    private static void mergeFacetItems(Facet.FacetBuilder builder, Facet sourceFacet) {
        // Initialize itemMap to store merged FacetItems
        Map<String, FacetItem.FacetItemBuilder> itemMap = new HashMap<>();

        // Collect existing FacetItems from the builder (handling potential null)
        List<FacetItem> existingValues = builder.build().getValues();
        if (existingValues != null) {
            for (FacetItem existingItem : existingValues) {
                String key = existingItem.getLabel() + "|" + existingItem.getValue();
                FacetItem.FacetItemBuilder itemBuilder = FacetItem.builder()
                        .label(existingItem.getLabel())
                        .value(existingItem.getValue())
                        .count(existingItem.getCount());
                itemMap.put(key, itemBuilder);
            }
        }

        // Merge FacetItems from the sourceFacet (handling potential null in sourceFacet.getValues())
        List<FacetItem> sourceValues = sourceFacet.getValues();
        if (sourceValues != null) {
            for (FacetItem incomingItem : sourceValues) {
                String key = incomingItem.getLabel() + "|" + incomingItem.getValue();
                if (itemMap.containsKey(key)) {
                    // Add counts if the item already exists
                    FacetItem.FacetItemBuilder existingBuilder = itemMap.get(key);
                    existingBuilder.count(existingBuilder.build().getCount() + incomingItem.getCount());
                } else {
                    // Otherwise, add the new item
                    FacetItem.FacetItemBuilder newItemBuilder = FacetItem.builder()
                            .label(incomingItem.getLabel())
                            .value(incomingItem.getValue())
                            .count(incomingItem.getCount());
                    itemMap.put(key, newItemBuilder);
                }
            }
        }

        // Update the builder's values with the merged items
        List<FacetItem> facetItems = new ArrayList<>(); // Ensure the builder is initialized with a new list
        for (FacetItem.FacetItemBuilder itemBuilder : itemMap.values()) {
            facetItems.add(itemBuilder.build());
        }
        builder.values(facetItems);
    }
}

