package org.uniprot.api.mapto.common.search;

import static org.uniprot.store.config.UniProtDataType.UNIREF;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToSearchResult;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Component
public class UniProtKBMapToSearchService implements MapToSearchService {
    private final UniprotQueryRepository uniprotQueryRepository;
    private final Map<UniProtDataType, List<String>> targetFields =
            Map.of(UNIREF, List.of("uniref_cluster_50", "uniref_cluster_90", "uniref_cluster_100"));
    private final Map<UniProtDataType, Function<UniProtDocument, List<String>>> targetMappings =
            Map.of(
                    UNIREF,
                    uniProtDocument ->
                            List.of(
                                    uniProtDocument.unirefCluster50,
                                    uniProtDocument.unirefCluster90,
                                    uniProtDocument.unirefCluster100));

    public UniProtKBMapToSearchService(UniprotQueryRepository uniprotQueryRepository) {
        this.uniprotQueryRepository = uniprotQueryRepository;
    }

    @Override
    public MapToSearchResult getTargetIds(String query, UniProtDataType target, String cursor) {
        SolrRequest solrRequest =
                SolrRequest.builder()
                        .query(query)
                        .rows(MAP_TO_PAGE_SIZE)
                        .fields(String.join(",", targetFields.get(target)))
                        .build();

        QueryResult<UniProtDocument> resultPage =
                uniprotQueryRepository.searchPage(solrRequest, cursor);
        CursorPage page = (CursorPage) resultPage.getPage();
        List<String> targetIds =
                resultPage
                        .getContent()
                        .map(targetMappings.get(target))
                        .flatMap(Collection::stream)
                        .toList();

        return new MapToSearchResult(targetIds, page);
    }
}
