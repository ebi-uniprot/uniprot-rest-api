package org.uniprot.api.rest.download.repository;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.PartialUpdate;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.download.model.DownloadJob;

@Component
@Profile({"asyncDownload"})
public class DownloadJobPartialUpdateRepositoryImpl implements DownloadJobPartialUpdateRepository {
    private final RedisKeyValueTemplate redisKeyValueTemplate;

    public DownloadJobPartialUpdateRepositoryImpl(RedisKeyValueTemplate redisKeyValueTemplate) {
        this.redisKeyValueTemplate = redisKeyValueTemplate;
    }

    @Override
    public void update(String jobId, Map<String, Object> fieldsToUpdate) {
        PartialUpdate<DownloadJob> partialUpdate = new PartialUpdate<>(jobId, DownloadJob.class);
        for (Map.Entry<String, Object> update : fieldsToUpdate.entrySet()) {
            partialUpdate = partialUpdate.set(update.getKey(), update.getValue());
        }
        redisKeyValueTemplate.update(partialUpdate);
    }
}