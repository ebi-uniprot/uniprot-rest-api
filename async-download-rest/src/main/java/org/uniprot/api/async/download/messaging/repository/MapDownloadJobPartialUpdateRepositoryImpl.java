package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;

@Component
public class MapDownloadJobPartialUpdateRepositoryImpl
        extends AbstractDownloadJobPartialUpdateRepository<MapDownloadJob>
        implements MapDownloadJobPartialUpdateRepository {
    public MapDownloadJobPartialUpdateRepositoryImpl(RedisKeyValueTemplate redisKeyValueTemplate) {
        super(redisKeyValueTemplate, MapDownloadJob.class);
    }
}
