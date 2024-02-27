package org.uniprot.api.async.download.queue.idmapping;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.queue.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.queue.common.DownloadConfigProperties;

@Component
@Profile({"asyncDownload"})
public class IdMappingAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public IdMappingAsyncDownloadFileHandler(
            DownloadConfigProperties idMappingDownloadConfigProperties) {
        super(idMappingDownloadConfigProperties);
    }
}
