package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;

@Component
public class UniRefDownloadJobPartialUpdateRepositoryImpl
        extends AbstractDownloadJobPartialUpdateRepository<UniRefDownloadJob>
        implements UniRefDownloadJobPartialUpdateRepository {
    public UniRefDownloadJobPartialUpdateRepositoryImpl(
            RedisKeyValueTemplate redisKeyValueTemplate) {
        super(redisKeyValueTemplate, UniRefDownloadJob.class);
    }
}
