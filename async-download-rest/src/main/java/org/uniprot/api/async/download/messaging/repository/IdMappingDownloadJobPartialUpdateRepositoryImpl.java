package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;

@Component
public class IdMappingDownloadJobPartialUpdateRepositoryImpl
        extends AbstractDownloadJobPartialUpdateRepository<IdMappingDownloadJob>
        implements IdMappingDownloadJobPartialUpdateRepository {
    public IdMappingDownloadJobPartialUpdateRepositoryImpl(
            RedisKeyValueTemplate redisKeyValueTemplate) {
        super(redisKeyValueTemplate, IdMappingDownloadJob.class);
    }
}
