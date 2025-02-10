package org.uniprot.api.common.repository.search.facet;

import java.util.List;

import lombok.AllArgsConstructor;
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
    // TODO create a builder with list a facets
    // add a static merge method to return single SolrStreamFacetResponse
}
