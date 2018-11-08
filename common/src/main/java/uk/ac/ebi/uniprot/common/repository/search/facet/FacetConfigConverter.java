package uk.ac.ebi.uniprot.common.repository.search.facet;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This interface is responsible to convert QueryResponse facets to a List<Facet> response model.
 * <p>
 * During the conversion it also add configuration label/properties from facet.property
 *
 * @author lgonzales
 */
// TODO: 08/11/18 test
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
        if (!CollectionUtils.isEmpty(queryResponse.getFacetFields())) {
            for (FacetField facetField : queryResponse.getFacetFields()) {
                // Iterating over all Query response Facets
                List<FacetItem> values = new ArrayList<>();
                if (!CollectionUtils.isEmpty(facetField.getValues())) {
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
