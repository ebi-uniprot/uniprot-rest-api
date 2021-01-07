package org.uniprot.api.uniprotkb.repository.search.impl;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.uniprotkb.service.PublicationFacetConfig2;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.publication.PublicationDocument;

@Repository
public class PublicationRepository extends SolrQueryRepository<PublicationDocument> {

    protected PublicationRepository(
            SolrClient solrClient,
            PublicationFacetConfig2 facetConfig2,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.publication,
                PublicationDocument.class,
                facetConfig2,
                requestConverter);
    }
}
