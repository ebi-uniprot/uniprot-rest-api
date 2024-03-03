package org.uniprot.api.async.download.messaging.repository;

import java.util.Map;

public interface DownloadJobPartialUpdateRepository {
    void update(String jobId, Map<String, Object> fieldsToUpdate);
}
