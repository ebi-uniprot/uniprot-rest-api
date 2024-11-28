package org.uniprot.api.async.download.messaging.consumer.processor.mapto.from;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBToUniRefFromIdRequestProcessor
        extends MapToFromIdRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBToUniRefFromIdRequestProcessor(
            MapToFileHandler fileHandler,
            UniProtEntryService uniProtEntryService,
            MapToJobService mapToJobService) {
        super(fileHandler, mapToJobService);
        this.uniProtEntryService = uniProtEntryService;
    }

    @Override
    protected Stream<String> streamIds(UniProtKBToUniRefDownloadRequest downloadRequest) {
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
