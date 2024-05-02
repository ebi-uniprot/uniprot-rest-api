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
 * This class load all Uniprot facet properties. The properties must have the prefix "facet.uniprot"
 *
 * <p>IMPORTANT: Make sure the the property structure in facet.properties follows {@link
 * FacetProperty} structure.
 *
 * @author lgonzales
 */
@Component
@Getter
@Setter
@PropertySource("classpath:uniprotkb.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class UniProtKBFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> uniprot = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return uniprot;
    }

    @Override
    public Collection<String> getFacetNames() {
        return uniprot.keySet();
    }
}
