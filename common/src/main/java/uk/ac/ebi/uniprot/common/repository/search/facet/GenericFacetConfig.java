package uk.ac.ebi.uniprot.common.repository.search.facet;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains generic facet configuration
 *
 * @author lgonzales
 */
@Getter
@Setter
public class GenericFacetConfig {

    private int mincount;

    private int limit;
}
