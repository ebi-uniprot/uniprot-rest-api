package org.uniprot.api.support.data.crossref.request;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

@Component
@Getter
@Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class CrossRefFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> crossref = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return crossref;
    }

    @Override
    public Collection<String> getFacetNames() {
        return crossref.keySet();
    }
}
