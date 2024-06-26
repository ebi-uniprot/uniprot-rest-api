package org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.id.SolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBSolrIdRequestProcessor
        extends SolrIdRequestProcessor<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBSolrIdRequestProcessor(
            UniProtKBFileHandler downloadFileHandler,
            UniProtKBJobService jobService,
            UniProtEntryService uniProtEntryService) {
        super(downloadFileHandler, jobService);
        this.uniProtEntryService = uniProtEntryService;
    }

    @Override
    protected long getSolrHits(UniProtKBDownloadRequest downloadRequest) {
        UniProtKBSearchRequest searchRequest = new UniProtKBSearchRequest();
        searchRequest.setQuery(downloadRequest.getQuery());
        searchRequest.setIncludeIsoform(downloadRequest.getIncludeIsoform());
        searchRequest.setSize(0);
        QueryResult<UniProtKBEntry> searchResults = uniProtEntryService.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }

    @Override
    protected Stream<String> streamIds(UniProtKBDownloadRequest downloadRequest) {
        return uniProtEntryService.streamIdsForDownload(downloadRequest);
    }
}
