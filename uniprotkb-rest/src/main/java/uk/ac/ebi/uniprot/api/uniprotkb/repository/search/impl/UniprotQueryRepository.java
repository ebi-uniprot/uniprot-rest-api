package uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;

import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.uniprot.UniProtDocument;

/**
 * Repository responsible to query SolrCollection.uniprot
 *
 * @author lgonzales
 */
@Repository
public class UniprotQueryRepository  extends SolrQueryRepository<UniProtDocument> {
    public UniprotQueryRepository(SolrTemplate solrTemplate, UniprotFacetConfig facetConverter, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.uniprot, UniProtDocument.class,facetConverter, requestConverter);
    }
}
