package org.uniprot.api.async.download.model.idmapping;

import java.time.LocalDateTime;

import lombok.Builder;

import org.springframework.data.redis.core.RedisHash;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

@RedisHash("idmapping")
public class IdMappingDownloadJob extends DownloadJob {

    private static final long serialVersionUID = -7048608562273428272L;

    @Builder
    public IdMappingDownloadJob(
            String id,
            JobStatus status,
            LocalDateTime created,
            LocalDateTime updated,
            String error,
            int retried,
            String query,
            String fields,
            String sort,
            String resultFile,
            String format,
            long totalEntries,
            long processedEntries,
            long updateCount) {
        super(
                id,
                status,
                created,
                updated,
                error,
                retried,
                query,
                fields,
                sort,
                resultFile,
                format,
                totalEntries,
                processedEntries,
                updateCount);
    }
}