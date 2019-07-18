package uk.ac.ebi.uniprot.api.common.repository.search.facet;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.IntervalFacet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.uniprot.common.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This interface is responsible to convert QueryResponse facets to a List<Facet> response model.
 * <p>
 * During the conversion it also add configuration label/properties from facet.property
 *
 * @author lgonzales
 */
public interface FacetConfigConverter extends Converter<QueryResponse, List<Facet>> {

    /**
     * This method is responsible to convert QueryResponse facets to a List<Facet> response model,
     * adding configured labels and properties
     *
     * @param queryResponse Solr query Response
     * @return List of Facet converted and configured.
     */
    @Override
    default List<Facet> convert(QueryResponse queryResponse) {
        List<Facet> facetResult = new ArrayList<>();
        if (Utils.notEmpty(queryResponse.getFacetFields())) {
            for (FacetField facetField : queryResponse.getFacetFields()) {
                // Iterating over all Query response Facets
                List<FacetItem> values = new ArrayList<>();
                if (Utils.notEmpty(facetField.getValues())) {
                    for (FacetField.Count count : facetField.getValues()) {
                        // Iterating over all query response facet items
                        if (count != null) {
                            //Adding add Facet Item to facet item list
                            values.add(FacetItem.builder()
                                               .value(count.getName())
                                               .label(getFacetItemLabel(facetField.getName(), count.getName()))
                                               .count(count.getCount())
                                               .build());
                        }
                    }
                }
                //Adding a facet to facet result list
                facetResult.add(Facet.builder()
                                        .name(facetField.getName())
                                        .label(getFacetLabel(facetField.getName()))
                                        .allowMultipleSelection(allowMultipleSelection(facetField.getName()))
                                        .values(values)
                                        .build());
            }
        }
        if (Utils.notEmpty(queryResponse.getIntervalFacets())) {
            for (IntervalFacet intervalFacet : queryResponse.getIntervalFacets()) {
                // Iterating over all Query response Interval Facets
                List<FacetItem> values = new ArrayList<>();
                if (Utils.notEmpty(intervalFacet.getIntervals())) {
                    for (IntervalFacet.Count count : intervalFacet.getIntervals()) {
                        // Iterating over all query response interval facet items
                        if (count != null) {
                            String queryTerm = count.getKey().replace(",", " TO ");
                            //Adding add Facet Item to facet item list
                            values.add(FacetItem.builder()
                                    .value(queryTerm)
                                    .label(getIntervalFacetItemLabel(intervalFacet.getField(), count.getKey()))
                                    .count((long) count.getCount())
                                    .build());
                        }
                    }
                }
                //Adding an Interval facet to facet result list
                facetResult.add(Facet.builder()
                        .name(intervalFacet.getField())
                        .label(getFacetLabel(intervalFacet.getField()))
                        .allowMultipleSelection(allowMultipleSelection(intervalFacet.getField()))
                        .values(values)
                        .build());
            }
        }
        return facetResult;
    }

    /**
     * This method returns Facet label for a facetName
     *
     * @param facetName facet name returned in Solr query response
     * @return facet label from facet.properties
     */
    default String getFacetLabel(String facetName) {
        String result = null;
        FacetProperty facetProperty = getFacetPropertyMap().getOrDefault(facetName, null);
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
    default String getFacetItemLabel(String facetName, String facetItem) {
        String result = null;
        FacetProperty facetProperty = getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && facetProperty.getValue() != null) {
            result = facetProperty.getValue().getOrDefault(facetItem, null);
        }
        return result;
    }


    /**
     * This method returns Facet item label for a facetName that is Interval Facet
     *
     * @param facetName        facet name returned in Solr query response
     * @param facetIntervalKey facet item key return in Solr query response
     * @return facet item label from facet.properties
     */
    default String getIntervalFacetItemLabel(String facetName, String facetIntervalKey) {
        String result = null;
        FacetProperty facetProperty = getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && facetProperty.getInterval() != null) {
            Optional<String> intervalPropertyKeyValue = facetProperty.getInterval().entrySet().stream()
                    .filter(item -> item.getValue().equalsIgnoreCase(facetIntervalKey))
                    .map(Map.Entry::getKey)
                    .findAny();
            if (intervalPropertyKeyValue.isPresent()) {
                result = getFacetItemLabel(facetName, intervalPropertyKeyValue.get());
            }
        }
        return result;
    }


    /**
     * this method returns allowMultipleSelection property for a facet name
     *
     * @param facetName facet name returned in Solr query response
     * @return allowMultipleSelection property from facet.properties
     */
    default boolean allowMultipleSelection(String facetName) {
        boolean result = false;
        FacetProperty facetProperty = getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null) {
            result = facetProperty.getAllowmultipleselection();
        }
        return result;
    }

    Map<String, FacetProperty> getFacetPropertyMap();

}
