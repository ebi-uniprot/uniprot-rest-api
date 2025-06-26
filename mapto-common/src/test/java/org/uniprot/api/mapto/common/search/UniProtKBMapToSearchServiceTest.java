package org.uniprot.api.mapto.common.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.uniprot.api.mapto.common.search.MapToSearchService.MAP_TO_PAGE_SIZE;
import static org.uniprot.store.config.UniProtDataType.UNIREF;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToSearchResult;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.service.request.UniProtKBRequestConverter;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@ExtendWith(MockitoExtension.class)
class UniProtKBMapToSearchServiceTest {
    public static final String CURSOR = "cursor";
    public static final String UNI_REF_CLUSTER_50_0 = "uniRefCluster50_0";
    public static final String UNI_REF_CLUSTER_90_0 = "uniRefCluster90_0";
    public static final String UNI_REF_CLUSTER_100_0 = "uniRefCluster100_0";
    private static final String UNI_REF_CLUSTER_50_1 = "uniRefCluster50_1";
    private static final String UNI_REF_CLUSTER_100_1 = "uniRefCluster100_1";
    public static final String QUERY = "query";
    private final UniProtKBSearchRequest searchRequest = new UniProtKBSearchRequest();
    @Mock private UniprotQueryRepository uniprotQueryRepository;
    @Mock private UniProtKBRequestConverter uniProtKBRequestConverter;
    @InjectMocks private UniProtKBMapToSearchService uniProtKBMapToSearchService;
    @Mock private QueryResult<UniProtDocument> queryResult;
    @Mock private CursorPage page;
    @Mock private MapToJob mapToJob;
    @Mock private SolrRequest solrRequest;
    private Stream<UniProtDocument> content;
    private final UniProtDocument uniProtDocument0 = new UniProtDocument();
    private final UniProtDocument uniProtDocument1 = new UniProtDocument();

    @BeforeEach
    void setUp() {
        lenient().when(mapToJob.getTargetDB()).thenReturn(UNIREF);
        lenient().when(mapToJob.getQuery()).thenReturn(QUERY);
        searchRequest.setQuery(mapToJob.getQuery());
        searchRequest.setSize(MAP_TO_PAGE_SIZE);
        searchRequest.setIncludeIsoform(
                Optional.ofNullable(mapToJob.getExtraParams().get("includeIsoform"))
                        .orElse("false"));
        searchRequest.setFields("uniref_cluster_50,uniref_cluster_90,uniref_cluster_100");
        when(uniProtKBRequestConverter.createSearchSolrRequest(searchRequest))
                .thenReturn(solrRequest);
        uniProtDocument0.unirefCluster50 = UNI_REF_CLUSTER_50_0;
        uniProtDocument0.unirefCluster90 = UNI_REF_CLUSTER_90_0;
        uniProtDocument0.unirefCluster100 = UNI_REF_CLUSTER_100_0;
        uniProtDocument1.unirefCluster50 = UNI_REF_CLUSTER_50_1;
        uniProtDocument1.unirefCluster90 = UNI_REF_CLUSTER_90_0;
        uniProtDocument1.unirefCluster100 = UNI_REF_CLUSTER_100_1;
    }

    @Test
    void getTargetIds_uniRef() {
        content = Stream.of(uniProtDocument0, uniProtDocument1);
        when(uniprotQueryRepository.searchPage(solrRequest, CURSOR)).thenReturn(queryResult);
        when(queryResult.getPage()).thenReturn(page);
        when(queryResult.getContent()).thenReturn(content);

        MapToSearchResult targetIds = uniProtKBMapToSearchService.getTargetIds(mapToJob, CURSOR);

        assertSame(page, targetIds.getPage());
        assertThat(
                targetIds.getTargetIds(),
                hasItems(
                        UNI_REF_CLUSTER_50_0,
                        UNI_REF_CLUSTER_90_0,
                        UNI_REF_CLUSTER_100_0,
                        UNI_REF_CLUSTER_50_1,
                        UNI_REF_CLUSTER_100_1));
    }

    @Test
    void getTargetIds_uniRefEmptyResults() {
        content = Stream.of();
        when(uniprotQueryRepository.searchPage(solrRequest, CURSOR)).thenReturn(queryResult);
        when(queryResult.getPage()).thenReturn(page);
        when(queryResult.getContent()).thenReturn(content);

        MapToSearchResult targetIds = uniProtKBMapToSearchService.getTargetIds(mapToJob, CURSOR);

        assertSame(page, targetIds.getPage());
        assertThat(targetIds.getTargetIds(), empty());
    }

    @Test
    void getTargetIds_uniRefNullValues() {
        uniProtDocument0.unirefCluster50 = null;
        uniProtDocument0.unirefCluster90 = null;
        uniProtDocument0.unirefCluster100 = null;
        content = Stream.of(uniProtDocument0);
        when(uniprotQueryRepository.searchPage(solrRequest, CURSOR)).thenReturn(queryResult);
        when(queryResult.getPage()).thenReturn(page);
        when(queryResult.getContent()).thenReturn(content);
        MapToSearchResult targetIds = uniProtKBMapToSearchService.getTargetIds(mapToJob, CURSOR);
        assertSame(page, targetIds.getPage());
        assertThat(targetIds.getTargetIds(), empty());
    }

    @Test
    void getTargetIds_uniRefNullValues2() {
        uniProtDocument0.unirefCluster50 = null;
        uniProtDocument0.unirefCluster90 = null;
        content = Stream.of(uniProtDocument0, uniProtDocument1);
        when(uniprotQueryRepository.searchPage(solrRequest, CURSOR)).thenReturn(queryResult);
        when(queryResult.getPage()).thenReturn(page);
        when(queryResult.getContent()).thenReturn(content);
        MapToSearchResult targetIds = uniProtKBMapToSearchService.getTargetIds(mapToJob, CURSOR);

        assertSame(page, targetIds.getPage());
        assertThat(
                targetIds.getTargetIds(),
                hasItems(UNI_REF_CLUSTER_100_0, UNI_REF_CLUSTER_50_1, UNI_REF_CLUSTER_100_1));
    }
}
