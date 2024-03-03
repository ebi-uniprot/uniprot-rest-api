package org.uniprot.api.async.download.messaging.result.idmapping;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;

@Component
@Profile({"asyncDownload"})
public class IdMappingAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public IdMappingAsyncDownloadFileHandler(
            DownloadConfigProperties idMappingDownloadConfigProperties) {
        super(idMappingDownloadConfigProperties);
    }
}
