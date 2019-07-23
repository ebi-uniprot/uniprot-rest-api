package uk.ac.ebi.uniprot.api.taxonomy.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;

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
