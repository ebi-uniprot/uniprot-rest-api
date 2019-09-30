package org.uniprot.api.common.repository.search.facet;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Facet Advanced Search Response Object
 *
 * @author lgonzales
 */
@Getter
@Builder
public class Facet {

    private String label;

    private String name;

    private boolean allowMultipleSelection;

    private List<FacetItem> values;
}
