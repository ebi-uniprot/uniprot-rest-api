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
 * @date: 20 Jun 2019
 */
@Component
@Getter
@Setter
@PropertySource("classpath:uniparc.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class UniParcFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> uniparc = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return uniparc;
    }

    public Collection<String> getFacetNames() {
        return uniparc.keySet();
    }
}
