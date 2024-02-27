package org.uniprot.api.async.download.queue.uniprotkb;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.queue.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.queue.common.DownloadConfigProperties;

@Component
@Profile({"asyncDownload"})
public class UniProtKBAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public UniProtKBAsyncDownloadFileHandler(
            DownloadConfigProperties uniProtKBDownloadConfigProperties) {
        super(uniProtKBDownloadConfigProperties);
    }
}
