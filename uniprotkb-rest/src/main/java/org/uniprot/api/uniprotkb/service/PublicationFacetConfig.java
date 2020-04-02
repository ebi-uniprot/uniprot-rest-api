package org.uniprot.api.uniprotkb.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrQueryUtil;

/**
 * @author lgonzales
 * @since 2019-12-13
 */
@Component
public class PublicationFacetConfig extends FacetConfig {

    private enum PUBLICATION_FACETS {
        source("Source"),
        category("Category"),
        scale("Scale");

        private String label;

        PUBLICATION_FACETS(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    static List<Facet> getFacets(List<PublicationEntry> publications, String requestFacets) {
        List<Facet> facets = new ArrayList<>();
        if (Utils.notNullNotEmpty(requestFacets)) {
            if (requestFacets.contains(PUBLICATION_FACETS.source.name())) {
                Facet source =
                        Facet.builder()
                                .label(PUBLICATION_FACETS.source.getLabel())
                                .name(PUBLICATION_FACETS.source.name())
                                .allowMultipleSelection(false)
                                .values(getSourceFacetValues(publications))
                                .build();
                facets.add(source);
            }

            if (requestFacets.contains(PUBLICATION_FACETS.category.name())) {
                Facet category =
                        Facet.builder()
                                .label(PUBLICATION_FACETS.category.getLabel())
                                .name(PUBLICATION_FACETS.category.name())
                                .allowMultipleSelection(false)
                                .values(getCategoryFacetValues(publications))
                                .build();
                facets.add(category);
            }

            if (requestFacets.contains(PUBLICATION_FACETS.scale.name())) {
                Facet scale =
                        Facet.builder()
                                .label(PUBLICATION_FACETS.scale.getLabel())
                                .name(PUBLICATION_FACETS.scale.name())
                                .allowMultipleSelection(false)
                                .values(getScaleFacetValues(publications))
                                .build();
                facets.add(scale);
            }
        }
        return facets;
    }

    static void applyFacetFilters(List<PublicationEntry> publications, PublicationRequest request) {
        String query = request.getQuery();
        if (Utils.notNullNotEmpty(query)) {
            if (SolrQueryUtil.hasFieldTerms(query, PUBLICATION_FACETS.source.name())) {
                String value;
                if (query.toLowerCase().contains("swiss")) {
                    value = "Swiss-Prot"; // //temporary work around
                } else {
                    value = SolrQueryUtil.getTermValue(query, PUBLICATION_FACETS.source.name());
                }
                publications.removeIf(
                        entry -> !entry.getPublicationSource().equalsIgnoreCase(value.trim()));
            }
            if (SolrQueryUtil.hasFieldTerms(query, PUBLICATION_FACETS.category.name())) {
                String value =
                        SolrQueryUtil.getTermValue(query, PUBLICATION_FACETS.category.name());
                publications.removeIf(
                        entry ->
                                entry.getCategories().stream()
                                        .noneMatch(
                                                cat -> {
                                                    return cat.equalsIgnoreCase(value.trim());
                                                }));
            }
            if (SolrQueryUtil.hasFieldTerms(query, PUBLICATION_FACETS.scale.name())) {
                String value = SolrQueryUtil.getTermValue(query, PUBLICATION_FACETS.scale.name());
                boolean isLargeScale = value.equalsIgnoreCase("Large");
                publications.removeIf(entry -> entry.isLargeScale() != isLargeScale);
            }
        }
    }

    private static List<FacetItem> getScaleFacetValues(List<PublicationEntry> publications) {
        Map<String, Long> scaleCountMap =
                publications.stream()
                        .map(PublicationFacetConfig::getPublicationScale)
                        .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        return buildFacetItems(scaleCountMap);
    }

    private static String getPublicationScale(PublicationEntry publicationEntry) {
        String value = "Small";
        if (publicationEntry.isLargeScale()) {
            value = "Large";
        }
        return value;
    }

    private static List<FacetItem> getCategoryFacetValues(List<PublicationEntry> publications) {
        Map<String, Long> categoryCountMap =
                publications.stream()
                        .filter(PublicationEntry::hasCategories)
                        .flatMap((publicationEntry) -> publicationEntry.getCategories().stream())
                        .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        return buildFacetItems(categoryCountMap);
    }

    private static List<FacetItem> getSourceFacetValues(List<PublicationEntry> publications) {
        Map<String, Long> sourceCountMap =
                publications.stream()
                        .filter(PublicationEntry::hasPublicationSource)
                        .map(PublicationEntry::getPublicationSource)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return buildFacetItems(sourceCountMap);
    }

    private static List<FacetItem> buildFacetItems(Map<String, Long> countMap) {
        List<FacetItem> result = new ArrayList<>();

        countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEachOrdered(
                        (entry) -> {
                            FacetItem item =
                                    FacetItem.builder()
                                            .value(entry.getKey())
                                            .count(entry.getValue())
                                            .build();
                            result.add(item);
                        });

        return result;
    }

    @Override
    public Collection<String> getFacetNames() {
        return Arrays.stream(PUBLICATION_FACETS.values())
                .map(PUBLICATION_FACETS::name)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return null;
    }
}
