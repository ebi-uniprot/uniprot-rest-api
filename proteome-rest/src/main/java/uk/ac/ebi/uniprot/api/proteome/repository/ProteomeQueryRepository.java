package uk.ac.ebi.uniprot.api.proteome.repository;

import org.springframework.data.solr.core.SolrTemplate;

import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/

public class ProteomeQueryRepository extends SolrQueryRepository<ProteomeDocument> {

	  public ProteomeQueryRepository(SolrTemplate solrTemplate, ProteomeFacetConfig facetConverter) {
	        super(solrTemplate, SolrCollection.proteome, ProteomeDocument.class,facetConverter);
	    }


}

