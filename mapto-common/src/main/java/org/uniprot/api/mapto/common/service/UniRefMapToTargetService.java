package org.uniprot.api.mapto.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

public class UniRefMapToTargetService extends MapToTargetService<UniRefEntryLight> {

    protected UniRefMapToTargetService(
            @Qualifier("uniRefEntryStoreStreamer") StoreStreamer<UniRefEntryLight> storeStreamer,
            @Qualifier("uniRefFacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RequestConverter requestConverter) {
        super(storeStreamer, tupleStream, facetConfig, requestConverter);
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF)
                .getSearchFieldItemByName("id")
                .getFieldName();
    }
}
