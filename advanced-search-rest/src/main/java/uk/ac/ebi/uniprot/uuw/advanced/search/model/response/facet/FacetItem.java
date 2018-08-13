package uk.ac.ebi.uniprot.uuw.advanced.search.model.response.facet;

import lombok.Builder;
import lombok.Getter;

/**
 * Facet Item Advanced Search Response Object
 *
 * @author lgonzales
 */
@Getter @Builder
public class FacetItem {

    private String label;

    private String value;

    private Long count;

}
