package org.uniprot.api.rest.download;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.request.DownloadRequest;

public interface DownloadResultWriter {

    void writeResult(
            DownloadRequest request,
            Path idFile,
            String jobId,
            MediaType contentType,
            StoreRequest storeRequest,
            String type)
            throws IOException;
}
