package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;

@Component
public class UniParcDownloadJobPartialUpdateRepositoryImpl
        extends AbstractDownloadJobPartialUpdateRepository<UniParcDownloadJob>
        implements UniParcDownloadJobPartialUpdateRepository {
    public UniParcDownloadJobPartialUpdateRepositoryImpl(
            RedisKeyValueTemplate redisKeyValueTemplate) {
        super(redisKeyValueTemplate, UniParcDownloadJob.class);
    }
}
