package org.uniprot.api.uniprotkb.common.service.publication;

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

@Component
@Getter
@Setter
@PropertySource("classpath:publication.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class PublicationFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> publication = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return publication;
    }

    @Override
    public Collection<String> getFacetNames() {
        return publication.keySet();
    }
}
