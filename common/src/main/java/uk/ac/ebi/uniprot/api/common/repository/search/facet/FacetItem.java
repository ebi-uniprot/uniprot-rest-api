package uk.ac.ebi.uniprot.api.common.repository.search.facet;

import lombok.Builder;
import lombok.Getter;

/**
 * Facet Item Advanced Search Response Object
 *
 * @author lgonzales
 */
@Getter
@Builder
public class FacetItem {

    private String label;

    private String value;

    private Long count;
}
