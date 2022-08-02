package org.uniprot.api.common.repository.stream.store.uniprotkb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
@Component
public class TaxonomyLineageFacetConfig extends FacetConfig {

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
