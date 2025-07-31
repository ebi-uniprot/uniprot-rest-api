package org.uniprot.api.mapto.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

@Service
public class UniRefMapToTargetService extends MapToTargetService<UniRefEntryLight> {

    protected UniRefMapToTargetService(
            @Qualifier("uniRefEntryStoreStreamer") StoreStreamer<UniRefEntryLight> storeStreamer,
            @Qualifier("uniRefFacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            UniRefFacetConfig uniRefEntryFacetConfig,
            RequestConverter uniRefRequestConverter,
            MapToJobService mapToJobService,
            MapToResultService mapToResultService,
            RdfStreamer uniRefRdfStreamer) {
        super(
                storeStreamer,
                tupleStream,
                uniRefEntryFacetConfig,
                uniRefRequestConverter,
                mapToJobService,
                mapToResultService,
                uniRefRdfStreamer);
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF)
                .getSearchFieldItemByName("id")
                .getFieldName();
    }
}
