package org.uniprot.api.subcell;

import org.springframework.data.solr.core.SolrTemplate;
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
            SolrTemplate solrTemplate, SolrRequestConverter requestConverter) {
        super(
                solrTemplate,
                SolrCollection.subcellularlocation,
                SubcellularLocationDocument.class,
                null,
                requestConverter);
    }
}
