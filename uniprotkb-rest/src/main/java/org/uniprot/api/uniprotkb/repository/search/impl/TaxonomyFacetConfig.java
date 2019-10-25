package org.uniprot.api.uniprotkb.repository.search.impl;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
@Component
public class TaxonomyFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> facet = new HashMap<>();

    @Override
    public Collection<String> getFacetNames() {
        return facet.keySet();
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return facet;
    }
}
