package org.uniprot.api.proteome.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 24 Apr 2019
 */
@Repository
public class ProteomeQueryRepository extends SolrQueryRepository<ProteomeDocument> {

    public ProteomeQueryRepository(
            SolrClient solrClient,
            ProteomeFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.proteome,
                ProteomeDocument.class,
                facetConfig,
                requestConverter);
    }
}
