package org.uniprot.api.keyword.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.keyword.KeywordRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

@Service
@Import(KeywordQueryBoostsConfig.class)
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {
    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause,
            QueryBoosts keywordQueryBoosts) {
        super(repository, keywordEntryConverter, keywordSortClause, keywordQueryBoosts, null);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.KEYWORD.getField("keyword_id").getName();
    }
}
