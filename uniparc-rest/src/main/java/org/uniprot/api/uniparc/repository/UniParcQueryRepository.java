package org.uniprot.api.uniparc.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Repository
public class UniParcQueryRepository extends SolrQueryRepository<UniParcDocument> {
    public UniParcQueryRepository(
            SolrClient solrClient,
            UniParcFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.uniparc,
                UniParcDocument.class,
                facetConfig,
                requestConverter);
    }
}
