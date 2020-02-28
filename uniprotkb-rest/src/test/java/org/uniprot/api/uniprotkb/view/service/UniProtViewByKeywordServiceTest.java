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
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.builder.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.builder.KeywordEntryKeywordBuilder;
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
        KeywordEntry growthArrest =
                new KeywordEntryBuilder().keyword(kw("Growth arrest", "KW-0338")).build();

        KeywordEntry parent =
                new KeywordEntryBuilder()
                        .keyword(kw("Catecholamine metabolism", "KW-9999"))
                        .build();

        KeywordEntry cellCycle =
                new KeywordEntryBuilder()
                        .keyword(kw("Cell cycle", "KW-0131"))
                        .parentsAdd(parent)
                        .build();

        KeywordEntry cellDivision =
                new KeywordEntryBuilder()
                        .keyword(kw("Cell division", "KW-0132"))
                        .parentsAdd(cellCycle)
                        .build();

        KeywordEntry catecholamineMetabolism =
                new KeywordEntryBuilder()
                        .keyword(kw("Catecholamine metabolism", "KW-0128"))
                        .parentsAdd(parent)
                        .build();

        KeywordEntry cellAdhesion =
                new KeywordEntryBuilder()
                        .keyword(kw("Cell adhesion", "KW-0130"))
                        .parentsAdd(parent)
                        .build();

        List<KeywordEntry> cellCycleChildren = Arrays.asList(cellDivision, growthArrest);
        cellCycle = KeywordEntryBuilder.from(cellCycle).childrenSet(cellCycleChildren).build();

        List<KeywordEntry> children =
                Arrays.asList(catecholamineMetabolism, cellAdhesion, cellCycle);
        parent = KeywordEntryBuilder.from(parent).childrenSet(children).build();

        when(keywordService.getByAccession(any())).thenReturn(parent);
    }

    private KeywordId kw(String id, String accession) {
        return new KeywordEntryKeywordBuilder().id(id).accession(accession).build();
    }
}
