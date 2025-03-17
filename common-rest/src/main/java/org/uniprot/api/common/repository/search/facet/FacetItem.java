package org.uniprot.api.common.repository.search.facet;

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

    @Override
    public String toString() {
        return "FacetItem{"
                + "label='"
                + label
                + '\''
                + ", value='"
                + value
                + '\''
                + ", count="
                + count
                + '}';
    }
}
