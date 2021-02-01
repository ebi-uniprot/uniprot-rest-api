package org.uniprot.api.common.repository.search.facet;

import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * This class represent facet configuration from facet.properties.
 *
 * <p>Please, check the header description at facet.properties
 *
 * @author lgonzales
 */
@Data
public class FacetProperty {

    @NotNull private String label;

    @NotNull private Boolean allowmultipleselection;

    /**
     * We specify the limit of values returned in each facet -1 limit, means, it will have no limit.
     */
    private int limit;

    private Map<String, String> interval;

    private Map<String, String> value;
}
