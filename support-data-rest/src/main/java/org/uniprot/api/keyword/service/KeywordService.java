package org.uniprot.api.keyword.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.uniprot.api.keyword.KeywordRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.field.KeywordField;

@Service
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            KeywordField.Search.content,
                            KeywordField.Search.id,
                            KeywordField.Search.getBoostFields());

    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause) {
        super(repository, keywordEntryConverter, keywordSortClause, handlerSupplier.get(), null);
    }

    @Override
    public KeywordEntry findByUniqueId(String uniqueId) {
        return getEntity(KeywordField.Search.keyword_id.name(), uniqueId);
    }
}
