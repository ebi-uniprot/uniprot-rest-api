package org.uniprot.api.async.download.messaging.result.common;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.common.repository.stream.store.StoreRequest;

public interface DownloadResultWriter {

    void writeResult(
            DownloadRequest request,
            DownloadJob downloadJob,
            Path idFile,
            MediaType contentType,
            StoreRequest storeRequest,
            String type)
            throws IOException;
}
