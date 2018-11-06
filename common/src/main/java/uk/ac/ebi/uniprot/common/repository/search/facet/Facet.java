package uk.ac.ebi.uniprot.common.repository.search.facet;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
