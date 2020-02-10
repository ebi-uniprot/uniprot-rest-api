package org.uniprot.api.keyword.service;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.uniprot.api.keyword.KeywordRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

@Service
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.KEYWORD, "content", "id", emptyList());

    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause) {
        super(repository, keywordEntryConverter, keywordSortClause, handlerSupplier.get(), null);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.KEYWORD.getField("keyword_id").getName();
    }
}
