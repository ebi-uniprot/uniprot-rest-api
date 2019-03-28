package uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class load all Uniprot facet properties. The properties must have the prefix "facet.uniprot"
 *
 * IMPORTANT: Make sure the the property structure in facet.properties follows {@link FacetProperty} structure.
 *
 * @author lgonzales
 */
@Component
@Getter @Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class UniprotFacetConfig extends GenericFacetConfig implements FacetConfigConverter {

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
