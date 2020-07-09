package org.uniprot.api.uniref.service;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@Service
public class UniRefEntryService extends BasicSearchService<UniRefDocument, UniRefEntry> {

    private final SearchFieldConfig searchFieldConfig;

    @Autowired
    public UniRefEntryService(
            SolrQueryRepository<UniRefDocument> repository, UniRefEntryConverter entryConverter) {
        super(repository, entryConverter);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF);
    }

    @Override
    protected String getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }

    @Override
    public QueryResult<UniRefEntry> search(SearchRequest request) {
        throw new UnsupportedOperationException(
                "UniRefEntryService does not support search, try to use UniRefLightSearchService");
    }

    @Override
    public Stream<UniRefEntry> download(SearchRequest request) {
        throw new UnsupportedOperationException(
                "UniRefEntryService does not support download, try to use UniRefLightSearchService");
    }
}
