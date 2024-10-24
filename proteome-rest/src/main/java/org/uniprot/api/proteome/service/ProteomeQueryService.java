package org.uniprot.api.proteome.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
@Import(ProteomeSolrQueryConfig.class)
public class ProteomeQueryService extends BasicSearchService<ProteomeDocument, ProteomeEntry> {
    public static final String PROTEOME_ID_FIELD = "upid";
    private final SearchFieldConfig fieldConfig;

    public ProteomeQueryService(
            ProteomeQueryRepository repository,
            SearchFieldConfig proteomeSearchFieldConfig,
            RequestConverter proteomeRequestConverter) {
        super(repository, new ProteomeEntryConverter(), proteomeRequestConverter);
        fieldConfig = proteomeSearchFieldConfig;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return fieldConfig.getSearchFieldItemByName(PROTEOME_ID_FIELD);
    }
}
