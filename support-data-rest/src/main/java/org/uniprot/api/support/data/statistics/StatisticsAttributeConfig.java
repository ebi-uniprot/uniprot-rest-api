package org.uniprot.api.support.data.statistics;

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
@PropertySource({"classpath:statistics.properties"})
@ConfigurationProperties(prefix = "statistics")
public class StatisticsAttributeConfig extends FacetConfig {

    private Map<String, FacetProperty> attributes = new HashMap<>();

    public Map<String, FacetProperty> getFacetPropertyMap() {
        return attributes;
    }

    public Collection<String> getFacetNames() {
        return attributes.keySet();
    }
}
