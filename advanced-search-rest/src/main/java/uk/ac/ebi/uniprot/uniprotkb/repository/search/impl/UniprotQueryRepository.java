package uk.ac.ebi.uniprot.uniprotkb.repository.search.impl;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;

import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.common.repository.search.SolrQueryRepository;

/**
 * Repository responsible to query SolrCollection.uniprot
 *
 * @author lgonzales
 */
@Repository
public class UniprotQueryRepository  extends SolrQueryRepository<UniProtDocument> {

    public UniprotQueryRepository(SolrTemplate solrTemplate, UniprotFacetConfig facetConverter) {
        super(solrTemplate, SolrCollection.uniprot, UniProtDocument.class,facetConverter);
    }

}
