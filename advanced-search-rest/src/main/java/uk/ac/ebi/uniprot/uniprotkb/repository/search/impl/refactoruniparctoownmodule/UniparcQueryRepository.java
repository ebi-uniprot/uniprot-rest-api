package uk.ac.ebi.uniprot.uniprotkb.repository.search.impl.refactoruniparctoownmodule;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.dataservice.document.uniparc.UniParcDocument;
import uk.ac.ebi.uniprot.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.common.repository.search.SolrQueryRepository;
/**
 * Repository responsible to query SolrCollection.uniparc
 *
 * @author lgonzales
 */
@Repository
public class UniparcQueryRepository extends SolrQueryRepository<UniParcDocument> {

    public UniparcQueryRepository(SolrTemplate solrTemplate, UniparcFacetConfig uniparcFacetConfig) {
        super(solrTemplate, SolrCollection.uniparc, UniParcDocument.class,uniparcFacetConfig);
    }

}
