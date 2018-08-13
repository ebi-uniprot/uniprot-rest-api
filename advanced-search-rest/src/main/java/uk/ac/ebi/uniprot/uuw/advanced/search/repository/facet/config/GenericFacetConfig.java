package uk.ac.ebi.uniprot.uuw.advanced.search.repository.facet.config;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains generic facet configuration
 *
 * @author lgonzales
 */
@Getter @Setter
public class GenericFacetConfig {

    private int mincount;

    private int limit;

}
