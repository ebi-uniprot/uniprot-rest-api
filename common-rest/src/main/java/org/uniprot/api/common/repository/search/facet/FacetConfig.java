package org.uniprot.api.common.repository.search.facet;

import java.util.Collection;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains generic facet configuration
 *
 * @author lgonzales
 */
@Getter
@Setter
public abstract class FacetConfig {
    private int mincount;

    private int limit;

    public abstract Collection<String> getFacetNames();

    public abstract Map<String, FacetProperty> getFacetPropertyMap();
}
