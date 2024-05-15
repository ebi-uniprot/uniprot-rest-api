package org.uniprot.api.help.centre.repository;

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
 * @since 07/07/2021
 */
@Component
@Getter
@Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class HelpCentreFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> helpcentre = new HashMap<>();

    @Override
    public Collection<String> getFacetNames() {
        return helpcentre.keySet();
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return helpcentre;
    }
}
