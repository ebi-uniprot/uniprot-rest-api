package org.uniprot.api.async.download.model.job.map;

import java.io.Serial;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisHash;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

import lombok.Builder;

@RedisHash("map")
public class MapDownloadJob extends DownloadJob {

    @Serial private static final long serialVersionUID = 3430390961923980454L;

    @Builder
    public MapDownloadJob(
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
