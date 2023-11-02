package org.uniprot.api.rest.download;

import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.request.DownloadRequest;

import java.io.IOException;
import java.nio.file.Path;

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
