package uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.SolrCollection;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.SolrQueryRepository;

/**
 * Repository responsible to query SolrCollection.uniprot
 *
 * @author lgonzales
 */
@Repository
public class UniprotQueryRespository  extends SolrQueryRepository<UniProtDocument> {

    public UniprotQueryRespository(SolrTemplate solrTemplate) {
        super(solrTemplate, SolrCollection.uniprot, UniProtDocument.class);
    }

}
