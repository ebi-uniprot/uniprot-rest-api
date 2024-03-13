package org.uniprot.api.async.download.messaging.result.uniref;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;

@Component
@Profile({"asyncDownload"})
public class UniRefAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public UniRefAsyncDownloadFileHandler(
            UniRefDownloadConfigProperties uniRefDownloadConfigProperties) {
        super(uniRefDownloadConfigProperties);
    }
}
