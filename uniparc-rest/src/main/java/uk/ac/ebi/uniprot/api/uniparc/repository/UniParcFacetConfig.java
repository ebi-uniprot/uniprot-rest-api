package uk.ac.ebi.uniprot.api.uniparc.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/
@Component
@Getter @Setter
@PropertySource("classpath:uniparc.facet.properties")
@ConfigurationProperties(prefix = "facet")
public class UniParcFacetConfig extends GenericFacetConfig implements FacetConfigConverter {

	 private Map<String, FacetProperty> uniparcFacets = new HashMap<>();
	
	@Override
	public Map<String, FacetProperty> getFacetPropertyMap() {
		return uniparcFacets;
	}
	 public Collection<String> getFacetNames() {
	        return uniparcFacets.keySet();
	    }

}

