package org.uniprot.api.support.data.keyword.service;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.api.support.data.keyword.KeywordRepository;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.keyword.KeywordDocument;

@Service
@Import(KeywordQueryBoostsConfig.class)
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {
    private static final String KEYWORD_ID_FIELD = "keyword_id";
    private final SearchFieldConfig fieldConfig;
    private final QueryProcessor queryProcessor;

    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause,
            QueryBoosts keywordQueryBoosts) {
        super(repository, keywordEntryConverter, keywordSortClause, keywordQueryBoosts, null);
        this.fieldConfig = SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.KEYWORD);
        this.queryProcessor =
                UniProtQueryProcessor.builder()
                        .queryProcessorPipeline(
                                new UniProtQueryNodeProcessorPipeline(
                                        getDefaultSearchOptimisedFieldItems()))
                        .build();
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.fieldConfig.getSearchFieldItemByName(KEYWORD_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }
}
