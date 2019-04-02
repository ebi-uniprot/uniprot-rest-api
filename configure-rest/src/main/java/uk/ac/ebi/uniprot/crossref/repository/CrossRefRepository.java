package uk.ac.ebi.uniprot.crossref.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.crossref.model.CrossRef;

@Repository
public class CrossRefRepository extends SolrQueryRepository<CrossRef> {

    public CrossRefRepository(SolrTemplate solrTemplate) {

        super(solrTemplate, SolrCollection.crossref, CrossRef.class, null);

    }

}
