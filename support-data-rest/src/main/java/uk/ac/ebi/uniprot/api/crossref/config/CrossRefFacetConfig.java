package uk.ac.ebi.uniprot.api.crossref.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Getter
@Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class CrossRefFacetConfig extends GenericFacetConfig implements FacetConfigConverter {

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