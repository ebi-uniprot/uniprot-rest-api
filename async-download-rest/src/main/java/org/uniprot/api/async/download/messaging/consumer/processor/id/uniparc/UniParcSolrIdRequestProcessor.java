package org.uniprot.api.async.download.messaging.consumer.processor.id.uniparc;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.id.SolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniparc.common.service.light.UniParcLightEntryService;
import org.uniprot.api.uniparc.common.service.request.UniParcSearchRequest;
import org.uniprot.core.uniparc.UniParcEntryLight;

@Component
public class UniParcSolrIdRequestProcessor
        extends SolrIdRequestProcessor<UniParcDownloadRequest, UniParcDownloadJob> {
    private final UniParcLightEntryService uniParcLightEntryService;

    protected UniParcSolrIdRequestProcessor(
            UniParcFileHandler downloadFileHandler,
            UniParcJobService jobService,
            UniParcLightEntryService uniParcLightEntryService) {
        super(downloadFileHandler, jobService);
        this.uniParcLightEntryService = uniParcLightEntryService;
    }

    @Override
    protected long getSolrHits(UniParcDownloadRequest downloadRequest) {
        UniParcSearchRequest searchRequest = new UniParcSearchRequest();
        searchRequest.setQuery(downloadRequest.getQuery());
        searchRequest.setSize(0);
        QueryResult<UniParcEntryLight> searchResults =
                uniParcLightEntryService.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }

    @Override
    protected Stream<String> streamIds(UniParcDownloadRequest downloadRequest) {
        return uniParcLightEntryService.streamIdsForDownload(downloadRequest);
    }
}
