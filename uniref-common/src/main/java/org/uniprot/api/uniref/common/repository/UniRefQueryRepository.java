package org.uniprot.api.uniref.common.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Repository
public class UniRefQueryRepository extends SolrQueryRepository<UniRefDocument> {
    public UniRefQueryRepository(
            SolrClient solrClient,
            UniRefFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.uniref,
                UniRefDocument.class,
                facetConfig,
                requestConverter);
    }
}
