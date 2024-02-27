package org.uniprot.api.async.download.queue.uniref;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.queue.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.queue.common.DownloadConfigProperties;

@Component
@Profile({"asyncDownload"})
public class UniRefAsyncDownloadFileHandler extends AsyncDownloadFileHandler {
    public UniRefAsyncDownloadFileHandler(DownloadConfigProperties uniRefDownloadConfigProperties) {
        super(uniRefDownloadConfigProperties);
    }
}
