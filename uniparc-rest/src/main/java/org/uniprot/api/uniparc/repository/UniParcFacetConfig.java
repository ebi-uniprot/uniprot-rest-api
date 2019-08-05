package org.uniprot.api.uniparc.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
public class UniParcFacetConfig extends FacetConfig {

	 private Map<String, FacetProperty> uniparcFacets = new HashMap<>();
	
	@Override
	public Map<String, FacetProperty> getFacetPropertyMap() {
		return uniparcFacets;
	}
	 public Collection<String> getFacetNames() {
	        return uniparcFacets.keySet();
	    }

}

