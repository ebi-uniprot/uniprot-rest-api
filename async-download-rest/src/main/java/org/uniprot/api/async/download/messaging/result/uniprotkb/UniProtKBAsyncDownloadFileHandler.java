package org.uniprot.api.async.download.messaging.result.uniprotkb;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;

@Component
@Profile({"asyncDownload"})
public class UniProtKBAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public UniProtKBAsyncDownloadFileHandler(
            UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties) {
        super(uniProtKBDownloadConfigProperties);
    }
}
