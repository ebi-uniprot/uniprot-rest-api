package org.uniprot.api.rest.download;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.request.StreamRequest;

public interface DownloadResultWriter {

    void writeResult(
            StreamRequest request,
            Path idFile,
            String jobId,
            MediaType contentType,
            StoreRequest storeRequest) throws IOException;
}
