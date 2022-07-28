package org.uniprot.api.common.repository.stream.store.uniprotkb;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
@Repository
public class TaxonomyLineageRepository extends SolrQueryRepository<TaxonomyDocument> {
    protected TaxonomyLineageRepository(
            SolrClient solrClient,
            TaxonomyLineageFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.taxonomy,
                TaxonomyDocument.class,
                facetConfig,
                requestConverter);
    }
}
