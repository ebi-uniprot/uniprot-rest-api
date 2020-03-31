package org.uniprot.api.uniprotkb.service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String SMALL_SCALE = "Small scale";
    private static final String LARGE_SCALE = "Large scale";
    private static final Pattern cleanValueRegex = Pattern.compile("[a-zA-Z]");

    private enum PUBLICATION_FACETS {
        source("Source"),
        category("Category"),
        study_type("Study type");

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

            if (requestFacets.contains(PUBLICATION_FACETS.study_type.name())) {
                Facet studyType =
                        Facet.builder()
                                .label(PUBLICATION_FACETS.study_type.getLabel())
                                .name(PUBLICATION_FACETS.study_type.name())
                                .allowMultipleSelection(false)
                                .values(getStudyTypeFacetValues(publications))
                                .build();
                facets.add(studyType);
            }
        }
        return facets;
    }

    static void applyFacetFilters(List<PublicationEntry> publications, PublicationRequest request) {
        String query = request.getQuery();
        if (Utils.notNullNotEmpty(query)) {
            if (SolrQueryUtil.hasFieldTerms(query, PUBLICATION_FACETS.source.name())) {
                String value = SolrQueryUtil.getTermValue(query, PUBLICATION_FACETS.source.name());
                publications.removeIf(entry -> filterSourceValues(value, entry));
            }
            if (SolrQueryUtil.hasFieldTerms(query, PUBLICATION_FACETS.category.name())) {
                String value =
                        SolrQueryUtil.getTermValue(query, PUBLICATION_FACETS.category.name());
                publications.removeIf(entry -> filterCategoryValues(value, entry));
            }
            if (SolrQueryUtil.hasFieldTerms(query, PUBLICATION_FACETS.study_type.name())) {
                String value =
                        SolrQueryUtil.getTermValue(query, PUBLICATION_FACETS.study_type.name());
                publications.removeIf(entry -> filterStudyTypeValues(value, entry));
            }
        }
    }

    private static boolean filterStudyTypeValues(String value, PublicationEntry entry) {
        boolean isLargeScale = value.equalsIgnoreCase(getCleanFacetValue(LARGE_SCALE));
        return entry.isLargeScale() != isLargeScale;
    }

    private static boolean filterCategoryValues(String value, PublicationEntry entry) {
        return entry.getCategories().stream()
                .noneMatch(
                        cat -> {
                            String cleanCategory = getCleanFacetValue(cat);
                            return cleanCategory.equalsIgnoreCase(value.trim());
                        });
    }

    private static boolean filterSourceValues(String value, PublicationEntry entry) {
        String cleanSource = getCleanFacetValue(entry.getPublicationSource());
        return !cleanSource.equalsIgnoreCase(value.trim());
    }

    private static List<FacetItem> getStudyTypeFacetValues(List<PublicationEntry> publications) {
        Map<String, Long> scaleCountMap =
                publications.stream()
                        .map(PublicationFacetConfig::getPublicationScale)
                        .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        return buildFacetItems(scaleCountMap);
    }

    private static String getPublicationScale(PublicationEntry publicationEntry) {
        String value = SMALL_SCALE;
        if (publicationEntry.isLargeScale()) {
            value = LARGE_SCALE;
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
                            String value = getCleanFacetValue(entry.getKey());
                            FacetItem item =
                                    FacetItem.builder()
                                            .label(entry.getKey())
                                            .value(value)
                                            .count(entry.getValue())
                                            .build();
                            result.add(item);
                        });

        return result;
    }

    private static String getCleanFacetValue(String value) {
        StringBuilder result = new StringBuilder();
        Matcher m = cleanValueRegex.matcher(value);
        while (m.find()) {
            result.append(m.group());
        }
        return result.toString().toLowerCase();
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
