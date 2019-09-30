package org.uniprot.api.uniparc.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Repository
public class UniParcQueryRepository extends SolrQueryRepository<UniParcDocument> {
    public UniParcQueryRepository(
            SolrTemplate solrTemplate,
            UniParcFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrTemplate,
                SolrCollection.uniparc,
                UniParcDocument.class,
                facetConfig,
                requestConverter);
    }
}
