package org.uniprot.api.async.download.messaging.consumer.processor.map.from;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

@Component
public class UniProtKBMapFromRequestProcessor
        extends MapFromRequestProcessor<UniProtKBMapDownloadRequest> {
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBMapFromRequestProcessor(
            MapFileHandler fileHandler, UniProtEntryService uniProtEntryService) {
        super(fileHandler);
        this.uniProtEntryService = uniProtEntryService;
    }

    @Override
    protected Stream<String> streamIds(UniProtKBMapDownloadRequest downloadRequest) {
        return uniProtEntryService.streamIdsForDownload(downloadRequest);
    }
}
