package org.uniprot.api.uniprotkb.service;

import org.uniprot.core.taxonomy.TaxonomyEntry;

/**
 *
 * @author jluo
 * @date: 16 Oct 2019
 *
*/

public interface TaxonomyService {
	TaxonomyEntry findById(long taxId);
}

