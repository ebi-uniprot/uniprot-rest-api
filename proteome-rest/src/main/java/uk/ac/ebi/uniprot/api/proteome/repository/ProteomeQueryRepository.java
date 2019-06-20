package uk.ac.ebi.uniprot.api.proteome.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;

import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/
@Repository
public class ProteomeQueryRepository extends SolrQueryRepository<ProteomeDocument> {

	  public ProteomeQueryRepository(SolrTemplate solrTemplate, ProteomeFacetConfig facetConverter,  SolrRequestConverter requestConverter) {
	        super(solrTemplate, SolrCollection.proteome, ProteomeDocument.class,facetConverter, requestConverter);
	    }
}

