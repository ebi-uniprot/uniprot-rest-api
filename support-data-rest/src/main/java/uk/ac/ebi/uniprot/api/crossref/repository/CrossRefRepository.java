package uk.ac.ebi.uniprot.api.crossref.repository;

import java.util.HashMap;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;

import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;


@Repository
public class CrossRefRepository extends SolrQueryRepository<CrossRefDocument> {

    public CrossRefRepository(SolrTemplate solrTemplate) {
        super(solrTemplate, SolrCollection.crossref, CrossRefDocument.class, () -> new HashMap<>());
    }

}
