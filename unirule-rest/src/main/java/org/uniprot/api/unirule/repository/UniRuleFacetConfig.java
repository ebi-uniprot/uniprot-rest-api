package org.uniprot.api.unirule.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Component
@Getter
@Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class UniRuleFacetConfig extends FacetConfig {
    private Map<String, FacetProperty> unirule = new HashMap<>();

    @Override
    public Collection<String> getFacetNames() {
        return this.unirule.keySet();
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return this.unirule;
    }
}
