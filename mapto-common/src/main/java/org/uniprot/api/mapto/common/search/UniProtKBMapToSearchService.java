package org.uniprot.api.mapto.common.search;

import static org.uniprot.store.config.UniProtDataType.UNIREF;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToSearchResult;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.service.request.UniProtKBRequestConverter;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Component
public class UniProtKBMapToSearchService implements MapToSearchService {
    private final UniprotQueryRepository repository;
    private final UniProtKBRequestConverter uniProtKBRequestConverter;
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

    public UniProtKBMapToSearchService(
            UniprotQueryRepository repository,
            UniProtKBRequestConverter uniProtKBRequestConverter) {
        this.repository = repository;
        this.uniProtKBRequestConverter = uniProtKBRequestConverter;
    }

    @Override
    public MapToSearchResult getTargetIds(MapToJob mapToJob, String cursor) {
        UniProtKBSearchRequest searchRequest = new UniProtKBSearchRequest();
        searchRequest.setQuery(mapToJob.getQuery());
        searchRequest.setSize(MAP_TO_PAGE_SIZE);
        searchRequest.setIncludeIsoform(
                Optional.ofNullable(mapToJob.getExtraParams().get(INCLUDE_ISOFORM))
                        .orElse("false"));
        UniProtDataType target = mapToJob.getTargetDB();
        searchRequest.setFields(String.join(",", targetFields.get(target)));

        QueryResult<UniProtDocument> resultPage =
                repository.searchPage(
                        uniProtKBRequestConverter.createSearchSolrRequest(searchRequest), cursor);
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
