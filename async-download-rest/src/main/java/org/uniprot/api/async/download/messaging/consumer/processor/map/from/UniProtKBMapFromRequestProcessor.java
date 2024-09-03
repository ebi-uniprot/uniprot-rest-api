package org.uniprot.api.async.download.messaging.consumer.processor.map.from;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBMapFromRequestProcessor
        extends MapFromRequestProcessor<UniProtKBToUniRefMapDownloadRequest> {
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBMapFromRequestProcessor(
            MapFileHandler fileHandler,
            UniProtEntryService uniProtEntryService,
            MapJobService mapJobService) {
        super(fileHandler, mapJobService);
        this.uniProtEntryService = uniProtEntryService;
    }

    @Override
    protected Stream<String> streamIds(UniProtKBToUniRefMapDownloadRequest downloadRequest) {
        return uniProtEntryService.streamIdsForDownload(downloadRequest);
    }

    @Override
    protected long getSolrHits(String query) {
        UniProtKBSearchRequest searchRequest = new UniProtKBSearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setSize(0);
        QueryResult<UniProtKBEntry> searchResults = uniProtEntryService.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }
}
