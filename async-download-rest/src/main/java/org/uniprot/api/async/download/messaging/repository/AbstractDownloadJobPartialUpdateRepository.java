package org.uniprot.api.async.download.messaging.repository;

import java.util.Map;

import org.springframework.data.redis.core.PartialUpdate;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.uniprot.api.async.download.model.job.DownloadJob;

public class AbstractDownloadJobPartialUpdateRepository<T extends DownloadJob>
        implements DownloadJobPartialUpdateRepository {
    private final RedisKeyValueTemplate redisKeyValueTemplate;

    private final Class<T> type;

    public AbstractDownloadJobPartialUpdateRepository(
            RedisKeyValueTemplate redisKeyValueTemplate, Class<T> type) {
        this.redisKeyValueTemplate = redisKeyValueTemplate;
        this.type = type;
    }

    @Override
    public void update(String jobId, Map<String, Object> fieldsToUpdate) {
        PartialUpdate<T> partialUpdate = new PartialUpdate<>(jobId, type);
        for (Map.Entry<String, Object> update : fieldsToUpdate.entrySet()) {
            partialUpdate = partialUpdate.set(update.getKey(), update.getValue());
        }
        redisKeyValueTemplate.update(partialUpdate);
    }
}
