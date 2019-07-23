package uk.ac.ebi.uniprot.api.literature.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Repository
public class LiteratureRepository extends SolrQueryRepository<LiteratureDocument> {

    protected LiteratureRepository(SolrTemplate solrTemplate, LiteratureFacetConfig facetConfig, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.literature, LiteratureDocument.class, facetConfig, requestConverter);
    }

}
