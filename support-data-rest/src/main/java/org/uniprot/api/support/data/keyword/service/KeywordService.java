package org.uniprot.api.support.data.keyword.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.support.data.keyword.KeywordRepository;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.keyword.KeywordDocument;

@Service
@Import(KeywordQueryBoostsConfig.class)
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {
    private SearchFieldConfig fieldConfig;

    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause,
            QueryBoosts keywordQueryBoosts) {
        super(repository, keywordEntryConverter, keywordSortClause, keywordQueryBoosts, null);
        this.fieldConfig = SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.KEYWORD);
    }

    @Override
    protected String getIdField() {
        return this.fieldConfig.getSearchFieldItemByName("keyword_id").getFieldName();
    }
}
