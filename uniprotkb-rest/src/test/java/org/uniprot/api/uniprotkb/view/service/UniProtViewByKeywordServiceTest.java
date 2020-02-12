package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.*;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.keyword.impl.KeywordEntryImpl;
import org.uniprot.core.cv.keyword.impl.KeywordImpl;
import org.uniprot.cv.keyword.KeywordService;

@ExtendWith(MockitoExtension.class)
class UniProtViewByKeywordServiceTest {
    @Mock private SolrClient solrClient;

    @Mock private KeywordService keywordService;
    private UniProtViewByKeywordService service;

    @BeforeEach
    void setup() {
        solrClient = Mockito.mock(SolrClient.class);
        keywordService = Mockito.mock(KeywordService.class);
        mockKeywordService();
        service = new UniProtViewByKeywordService(solrClient, "uniprot", keywordService);
    }

    @Test
    void test() throws IOException, SolrServerException {
        Map<String, Long> counts = new HashMap<>();
        counts.put("KW-0128", 5L);
        counts.put("KW-0130", 45L);
        counts.put("KW-0131", 102L);
        MockServiceHelper.mockServiceQueryResponse(solrClient, "keyword_id", counts);
        List<ViewBy> viewBys = service.get("", "KW-9999");
        assertEquals(3, viewBys.size());
        ViewBy viewBy1 =
                MockServiceHelper.createViewBy(
                        "KW-0128",
                        "Catecholamine metabolism",
                        5L,
                        UniProtViewByKeywordService.URL_PREFIX + "KW-0128",
                        false);
        assertTrue(viewBys.contains(viewBy1));
        ViewBy viewBy2 =
                MockServiceHelper.createViewBy(
                        "KW-0130",
                        "Cell adhesion",
                        45L,
                        UniProtViewByKeywordService.URL_PREFIX + "KW-0130",
                        false);
        assertTrue(viewBys.contains(viewBy2));
        ViewBy viewBy3 =
                MockServiceHelper.createViewBy(
                        "KW-0131",
                        "Cell cycle",
                        102L,
                        UniProtViewByKeywordService.URL_PREFIX + "KW-0131",
                        true);
        assertTrue(viewBys.contains(viewBy3));
    }

    void mockKeywordService() {
        KeywordEntryImpl keyword = new KeywordEntryImpl();
        keyword.setKeyword(new KeywordImpl("Catecholamine metabolism", "KW-9999"));
        Set<KeywordEntry> parents = Collections.singleton(keyword);
        KeywordEntryImpl kw1 = new KeywordEntryImpl();
        kw1.setKeyword(new KeywordImpl("Catecholamine metabolism", "KW-0128"));
        kw1.setParents(parents);
        KeywordEntryImpl kw2 = new KeywordEntryImpl();
        kw2.setKeyword(new KeywordImpl("Cell adhesion", "KW-0130"));
        kw2.setParents(parents);

        KeywordEntryImpl kw3 = new KeywordEntryImpl();
        kw3.setKeyword(new KeywordImpl("Cell cycle", "KW-0131"));
        kw3.setParents(parents);
        List<KeywordEntry> kws = new ArrayList<>();
        kws.add(kw1);
        kws.add(kw2);
        kws.add(kw3);
        Set<KeywordEntry> kws3 = Collections.singleton(kw3);
        KeywordEntryImpl kw31 = new KeywordEntryImpl();
        kw31.setKeyword(new KeywordImpl("Cell division", "KW-0132"));
        kw31.setParents(kws3);
        KeywordEntryImpl kw32 = new KeywordEntryImpl();
        kw32.setKeyword(new KeywordImpl("Growth arrest", "KW-0338"));
        List<KeywordEntry> kws33 = new ArrayList<>();
        kws33.add(kw31);
        kws33.add(kw32);
        kw31.setParents(kws3);
        kw3.setChildren(kws33);
        keyword.setChildren(kws);

        when(keywordService.getByAccession(any())).thenReturn(keyword);
    }
}
