package org.uniprot.api.uniref.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 *
 * @author jluo
 * @date: 20 Aug 2019
 *
*/
@Repository
public class UniRefQueryRepository extends SolrQueryRepository<UniRefDocument> {
    public UniRefQueryRepository(SolrTemplate solrTemplate, UniRefFacetConfig facetConfig, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.uniref, UniRefDocument.class, facetConfig, requestConverter);
    }

}

