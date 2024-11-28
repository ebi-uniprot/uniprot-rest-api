package org.uniprot.api.async.download.messaging.consumer.processor.uniref.id;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.SolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefSolrIdRequestProcessor
        extends SolrIdRequestProcessor<UniRefDownloadRequest, UniRefDownloadJob> {
    private final UniRefEntryLightService uniRefEntryLightService;

    protected UniRefSolrIdRequestProcessor(
            UniRefFileHandler downloadFileHandler,
            UniRefJobService jobService,
            UniRefEntryLightService uniRefEntryLightService) {
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
