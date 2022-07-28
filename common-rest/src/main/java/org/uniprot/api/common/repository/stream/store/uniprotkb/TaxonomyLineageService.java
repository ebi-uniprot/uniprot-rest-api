package org.uniprot.api.common.repository.stream.store.uniprotkb;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyLineage;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
public interface TaxonomyLineageService {
    TaxonomyEntry findById(long taxId);

    Map<Long, List<TaxonomyLineage>> findByIds(Set<Long> taxIds);
}
