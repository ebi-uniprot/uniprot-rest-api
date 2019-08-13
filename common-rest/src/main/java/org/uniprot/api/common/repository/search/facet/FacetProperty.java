package org.uniprot.api.common.repository.search.facet;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * This class represent facet configuration from facet.properties.
 * <p>
 * Please, check the header description at facet.properties
 *
 * @author lgonzales
 */
@Data
public class FacetProperty {

    @NotNull
    private String label;

    @NotNull
    private Boolean allowmultipleselection;

    private int limit;

    private Map<String, String> interval;

    private Map<String, String> value;
}
