package org.uniprot.api.async.download.messaging.repository;

import java.util.Map;

import org.springframework.data.redis.core.PartialUpdate;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.uniprot.api.async.download.model.job.DownloadJob;

public class AbstractDownloadJobPartialUpdateRepository<R extends DownloadJob>
        implements DownloadJobPartialUpdateRepository {
    private final RedisKeyValueTemplate redisKeyValueTemplate;

    private final Class<R> type;

    public AbstractDownloadJobPartialUpdateRepository(
            RedisKeyValueTemplate redisKeyValueTemplate, Class<R> type) {
        this.redisKeyValueTemplate = redisKeyValueTemplate;
        this.type = type;
    }

    @Override
    public void update(String jobId, Map<String, Object> fieldsToUpdate) {
        PartialUpdate<R> partialUpdate = new PartialUpdate<>(jobId, type);
        for (Map.Entry<String, Object> update : fieldsToUpdate.entrySet()) {
            partialUpdate = partialUpdate.set(update.getKey(), update.getValue());
        }
        redisKeyValueTemplate.update(partialUpdate);
    }
}
