package org.uniprot.api.aa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.aa.repository.ArbaQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.arba.ArbaDocument;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Service
public class ArbaService extends BasicSearchService<ArbaDocument, UniRuleEntry> {

    public static final String ARBA_ID_FIELD = "rule_id";
    private final SearchFieldConfig searchFieldConfig;

    @Autowired
    public ArbaService(
            ArbaQueryRepository repository,
            ArbaDocumentConverter arbaDocumentConverter,
            SearchFieldConfig arbaSearchFieldConfig,
            RequestConverter arbaRequestConverter) {
        super(repository, arbaDocumentConverter, arbaRequestConverter);
        this.searchFieldConfig = arbaSearchFieldConfig;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(ARBA_ID_FIELD);
    }
}
