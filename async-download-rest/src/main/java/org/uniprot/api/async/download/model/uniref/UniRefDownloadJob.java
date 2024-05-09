package org.uniprot.api.async.download.model.uniref;

import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisHash;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

import lombok.Builder;

@RedisHash("uniref")
public class UniRefDownloadJob extends DownloadJob {

    private static final long serialVersionUID = 3430390961923980454L;

    @Builder
    public UniRefDownloadJob(
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
