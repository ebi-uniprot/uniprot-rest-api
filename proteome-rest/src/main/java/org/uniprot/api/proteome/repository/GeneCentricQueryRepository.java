package org.uniprot.api.proteome.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

/**
 * @author jluo
 * @date: 17 May 2019
 */
@Repository
public class GeneCentricQueryRepository extends SolrQueryRepository<GeneCentricDocument> {

    public GeneCentricQueryRepository(
            SolrClient solrClient,
            GeneCentricFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.genecentric,
                GeneCentricDocument.class,
                facetConfig,
                requestConverter);
    }
}
