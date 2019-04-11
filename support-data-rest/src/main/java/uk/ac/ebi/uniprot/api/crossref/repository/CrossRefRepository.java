package uk.ac.ebi.uniprot.api.crossref.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRef;

import java.util.HashMap;

@Repository
public class CrossRefRepository extends SolrQueryRepository<CrossRef> {

    public CrossRefRepository(SolrTemplate solrTemplate) {
        super(solrTemplate, SolrCollection.crossref, CrossRef.class, () -> new HashMap<>());
    }

}
