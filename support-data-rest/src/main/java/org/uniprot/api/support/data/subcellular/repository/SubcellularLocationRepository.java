package org.uniprot.api.support.data.subcellular.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Repository
public class SubcellularLocationRepository
        extends SolrQueryRepository<SubcellularLocationDocument> {

    protected SubcellularLocationRepository(
            SolrClient solrClient, SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.subcellularlocation,
                SubcellularLocationDocument.class,
                null,
                requestConverter);
    }
}
