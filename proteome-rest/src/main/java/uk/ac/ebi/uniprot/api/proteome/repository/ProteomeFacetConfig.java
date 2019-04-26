package uk.ac.ebi.uniprot.api.proteome.repository;

import java.util.Collections;
import java.util.Map;

import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/

public class ProteomeFacetConfig extends GenericFacetConfig implements FacetConfigConverter {

	@Override
	public Map<String, FacetProperty> getFacetPropertyMap() {
		return Collections.emptyMap();
	}

}

