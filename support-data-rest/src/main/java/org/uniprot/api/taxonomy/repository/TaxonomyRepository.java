package org.uniprot.api.taxonomy.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 *
 * @author lgonzales
 */
@Repository
public class TaxonomyRepository extends SolrQueryRepository<TaxonomyDocument> {
    protected TaxonomyRepository(SolrTemplate solrTemplate, TaxonomyFacetConfig facetConfig, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.taxonomy, TaxonomyDocument.class, facetConfig, requestConverter);
    }
}
