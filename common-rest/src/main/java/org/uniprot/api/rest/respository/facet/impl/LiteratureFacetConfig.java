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
 * @author lgonzales
 * @since 2019-07-08
 */
@Component
@Getter
@Setter
@PropertySource("classpath:literature.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class LiteratureFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> literature = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return literature;
    }

    public Collection<String> getFacetNames() {
        return literature.keySet();
    }
}
