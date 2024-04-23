package org.uniprot.api.async.download.refactor.consumer.processor.id.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.id.SolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.util.stream.Stream;

@Component
public class UniRefSolrIdRequestProcessor extends SolrIdRequestProcessor<UniRefDownloadRequest, UniRefDownloadJob> {
    private final UniRefEntryLightService uniRefEntryLightService;

    protected UniRefSolrIdRequestProcessor(UniRefAsyncDownloadFileHandler downloadFileHandler, UniRefJobService jobService, UniRefEntryLightService uniRefEntryLightService) {
        super(downloadFileHandler, jobService);
        this.uniRefEntryLightService = uniRefEntryLightService;
    }

    @Override
    protected long getSolrHits(UniRefDownloadRequest downloadRequest) {
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setQuery(downloadRequest.getQuery());
        searchRequest.setSize(0);
        QueryResult<UniRefEntryLight> searchResults = uniRefEntryLightService.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }

    @Override
    protected Stream<String> streamIds(UniRefDownloadRequest downloadRequest) {
        return uniRefEntryLightService.streamIdsForDownload(downloadRequest);
    }
}
