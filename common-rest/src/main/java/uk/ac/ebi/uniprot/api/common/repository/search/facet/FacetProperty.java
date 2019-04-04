package uk.ac.ebi.uniprot.api.common.repository.search.facet;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * This class represent facet configuration from facet.properties.
 * <p>
 * Please, check the description at facet.properties
 *
 * @author lgonzales
 */
@Data
public class FacetProperty {

    @NotNull
    private String label;

    @NotNull
    private Boolean allowmultipleselection;

    private Map<String, String> value;
}
