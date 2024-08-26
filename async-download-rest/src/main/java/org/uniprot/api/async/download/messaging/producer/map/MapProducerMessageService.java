package org.uniprot.api.async.download.messaging.producer.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.mq.map.MapMessagingService;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import java.time.LocalDateTime;

@Slf4j
public abstract class MapProducerMessageService<T extends MapDownloadRequest>
        extends SolrProducerMessageService<T, MapDownloadJob> {
    private final MapJobService jobService;

    protected MapProducerMessageService(
            MapJobService jobService,
            MessageConverter messageConverter,
            MapMessagingService messagingService,
            HashGenerator<T> hashGenerator,
            MapFileHandler asyncDownloadFileHandler,
            MapJobSubmissionRules<T> asyncDownloadSubmissionRules) {
        super(
                jobService,
                messageConverter,
                messagingService,
                hashGenerator,
                asyncDownloadFileHandler,
                asyncDownloadSubmissionRules);
        this.jobService = jobService;
    }

    @Override
    protected void createDownloadJob(String jobId, T request) {
        MapDownloadJob.MapDownloadJobBuilder jobBuilder = MapDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(request.getQuery())
                .fields(request.getFields())
                .sort(request.getSort())
                .format(request.getFormat())
                .created(now)
                .updated(now);
        jobService.save(jobBuilder.build());
        log.info("Job with jobId {} created in redis", jobId);
    }
}
