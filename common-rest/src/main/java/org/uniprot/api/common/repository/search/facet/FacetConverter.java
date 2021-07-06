package org.uniprot.api.common.repository.search.facet;

import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.uniprot.core.util.Utils;

public abstract class FacetConverter<F, T> implements Converter<F, T> {

    protected abstract FacetConfig getFacetConfig();

    /**
     * This method returns Facet label for a facetName
     *
     * @param facetName facet name returned in Solr query response
     * @return facet label from facet.properties
     */
    protected String getFacetLabel(String facetName) {
        String result = null;
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && facetProperty.getLabel() != null) {
            result = facetProperty.getLabel();
        }
        return result;
    }

    /**
     * This method returns Facet label for a facetName
     *
     * @param facetName facet name returned in Solr query response
     * @param facetItem facet item value return in Solr query response
     * @return facet item label from facet.properties
     */
    protected String getFacetItemLabel(String facetName, String facetItem) {
        String result = null;
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && facetProperty.getValue() != null) {
            result = facetProperty.getValue().getOrDefault(facetItem.toLowerCase(), null);
        }
        return result;
    }

    /**
     * this method returns allowMultipleSelection property for a facet name
     *
     * @param facetName facet name returned in Solr query response
     * @return allowMultipleSelection property from facet.properties
     */
    protected boolean allowMultipleSelection(String facetName) {
        boolean result = false;
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null) {
            result = facetProperty.getAllowmultipleselection();
        }
        return result;
    }

    /**
     * this method returns sort property for a facet value
     *
     * @param facetName facet name returned in Solr query response
     * @return sort property from facet.properties
     */
    protected String getSort(String facetName) {
        String result = "asc";
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null) {
            result = facetProperty.getSort();
        }
        return result;
    }

    /**
     * this method returns if is an interval/range facet
     *
     * @param facetName facet name returned in Solr query response
     * @return true if is an interval facet
     */
    protected boolean isIntervalFacet(String facetName) {
        boolean result = false;
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && Utils.notNullNotEmpty(facetProperty.getInterval())) {
            result = true;
        }
        return result;
    }

    /**
     * This method returns Facet item label for a facetName that is Interval Facet
     *
     * @param facetName facet name returned in Solr query response
     * @param facetIntervalKey facet item key return in Solr query response
     * @return facet item label from facet.properties
     */
    protected String getIntervalFacetItemLabel(String facetName, String facetIntervalKey) {
        String result = null;
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && facetProperty.getInterval() != null) {
            Optional<String> intervalPropertyKeyValue =
                    facetProperty.getInterval().entrySet().stream()
                            .filter(item -> item.getValue().equalsIgnoreCase(facetIntervalKey))
                            .map(Map.Entry::getKey)
                            .findAny();
            if (intervalPropertyKeyValue.isPresent()) {
                result = getFacetItemLabel(facetName, intervalPropertyKeyValue.get());
            }
        }
        return result;
    }
}
