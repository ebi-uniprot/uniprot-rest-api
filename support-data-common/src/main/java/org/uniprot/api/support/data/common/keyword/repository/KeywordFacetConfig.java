package org.uniprot.api.support.data.common.keyword.repository;

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
 * @since 12/03/2021
 */
@Component
@Getter
@Setter
@PropertySource("classpath:keyword.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class KeywordFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> keyword = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return keyword;
    }

    public Collection<String> getFacetNames() {
        return keyword.keySet();
    }
}
