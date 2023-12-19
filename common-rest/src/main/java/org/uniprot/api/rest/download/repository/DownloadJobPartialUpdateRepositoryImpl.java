package org.uniprot.api.rest.download.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.PartialUpdate;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.download.model.DownloadJob;

import java.util.Map;

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
        fieldsToUpdate.keySet().forEach(field -> partialUpdate.set(field, fieldsToUpdate.get(field)));
        redisKeyValueTemplate.update(partialUpdate);
    }
}
