package org.uniprot.api.async.download.messaging.result.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;

@Component
public class UniRefAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public UniRefAsyncDownloadFileHandler(
            UniRefDownloadConfigProperties uniRefDownloadConfigProperties) {
        super(uniRefDownloadConfigProperties);
    }
}
