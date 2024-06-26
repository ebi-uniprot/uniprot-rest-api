package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;

@Component
public class UniProtKBDownloadJobPartialUpdateRepositoryImpl
        extends AbstractDownloadJobPartialUpdateRepository<UniProtKBDownloadJob>
        implements UniProtKBDownloadJobPartialUpdateRepository {
    public UniProtKBDownloadJobPartialUpdateRepositoryImpl(
            RedisKeyValueTemplate redisKeyValueTemplate) {
        super(redisKeyValueTemplate, UniProtKBDownloadJob.class);
    }
}
