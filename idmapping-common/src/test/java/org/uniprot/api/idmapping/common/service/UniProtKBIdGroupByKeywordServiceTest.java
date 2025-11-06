package org.uniprot.api.idmapping.common.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByKeywordService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByKeywordService;
import org.uniprot.core.cv.keyword.KeywordEntry;

@ExtendWith(MockitoExtension.class)
class UniProtKBIdGroupByKeywordServiceTest extends UniProtKBIdGroupByServiceTest<KeywordEntry> {
    @Mock private GroupByKeywordService groupByKeywordService;
    @Mock private KeywordEntry keywordEntry0;
    @Mock private KeywordEntry keywordEntry1;
    @Mock private KeywordEntry keywordEntry2;
    @Mock private KeywordEntry keywordEntry3;
    @Mock private KeywordEntry keywordEntry4;

    @BeforeEach
    void setUp() {
        idGroupByService =
                new UniProtKBIdGroupByKeywordService(groupByKeywordService, uniProtEntryService);
        groupByService = groupByKeywordService;
        init();
        when(groupByKeywordService.getKeywordFacetCounts(anyList()))
                .thenAnswer(in -> in.getArguments()[0]);
    }

    @Override
    protected void prepareForEmptyParent() {
        when(groupByKeywordService.getInitialEntries(null))
                .thenReturn(List.of(keywordEntry0, keywordEntry1, keywordEntry2));
    }

    @Override
    protected void prepareWithParent() {
        when(groupByKeywordService.getInitialEntries(PARENT))
                .thenReturn(List.of(keywordEntry0, keywordEntry1, keywordEntry2));
    }

    @Override
    protected List<KeywordEntry> getInitialEntries() {
        return List.of(keywordEntry0, keywordEntry1, keywordEntry2);
    }

    @Override
    protected List<KeywordEntry> getChildEntries() {
        return List.of(keywordEntry3, keywordEntry4);
    }
}
