package org.uniprot.api.rest.download.repository;

import java.util.Map;

public interface DownloadJobPartialUpdateRepository {
    void update(String jobId, Map<String, Object> fieldsToUpdate);
}
