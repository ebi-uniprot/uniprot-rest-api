package org.uniprot.api.async.download.messaging.consumer.processor.mapto.to;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefMapToIdRequestProcessor
        extends MapToIdRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    private final UniRefEntryLightService uniRefEntryLightService;

    protected UniRefMapToIdRequestProcessor(
            MapToFileHandler fileHandler,
            MapToJobService jobService,
            UniRefEntryLightService uniRefEntryLightService) {
        super(fileHandler, jobService);
        this.uniRefEntryLightService = uniRefEntryLightService;
    }

    @Override
    protected long getSolrHits(String query) {
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setSize(0);
        QueryResult<UniRefEntryLight> searchResults = uniRefEntryLightService.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }

    @Override
    protected String getQuery(Stream<String> ids) {
        return "uniprot_id: (" + ids.collect(Collectors.joining(" OR ")) + ")";
    }

    @Override
    protected Stream<String> mapIds(String query) {
        UniRefDownloadRequest uniRefDownloadRequest = new UniRefDownloadRequest();
        uniRefDownloadRequest.setQuery(query);
        return uniRefEntryLightService.streamIdsForDownload(uniRefDownloadRequest);
    }
}
