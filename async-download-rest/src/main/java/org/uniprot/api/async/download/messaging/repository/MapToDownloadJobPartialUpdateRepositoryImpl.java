package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;

@Component
public class MapToDownloadJobPartialUpdateRepositoryImpl
        extends AbstractDownloadJobPartialUpdateRepository<MapToDownloadJob>
        implements MapToDownloadJobPartialUpdateRepository {
    public MapToDownloadJobPartialUpdateRepositoryImpl(
            RedisKeyValueTemplate redisKeyValueTemplate) {
        super(redisKeyValueTemplate, MapToDownloadJob.class);
    }
}