package org.uniprot.api.rest.respository.facet.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
@Getter
@Setter
@PropertySource("classpath:uniref.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class UniRefFacetConfig extends FacetConfig {
    private Map<String, FacetProperty> uniref = new HashMap<>();

    @Override
    public Collection<String> getFacetNames() {
        return uniref.keySet();
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return uniref;
    }
}
