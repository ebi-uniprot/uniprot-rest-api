package org.uniprot.api.uniref.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.DownloadResultWriter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.AbstractMessageListener;
import org.uniprot.api.rest.download.queue.AsyncDownloadQueueConfigProperties;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniref.service.UniRefEntryLightService;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author tibrahim
 * @created 16/08/2023
 */
@Profile({"live", "asyncDownload"})
@Service("DownloadListener")
@Slf4j
public class UniRefMessageListener extends AbstractMessageListener implements MessageListener {
    private static final String UNIREF_DATA_TYPE = "uniref";
    private final UniRefEntryLightService service;

    public UniRefMessageListener(
            MessageConverter converter,
            DownloadConfigProperties downloadConfigProperties,
            AsyncDownloadQueueConfigProperties asyncDownloadQueueConfigProperties,
            DownloadJobRepository jobRepository,
            DownloadResultWriter downloadResultWriter,
            RabbitTemplate rabbitTemplate,
            UniRefEntryLightService service) {
        super(
                converter,
                downloadConfigProperties,
                asyncDownloadQueueConfigProperties,
                jobRepository,
                downloadResultWriter,
                rabbitTemplate);
        this.service = service;
    }

    @Override
    protected void updateStatusAndWriteResult(
            Message message, DownloadJob downloadJob, DownloadRequest request, Path idsFile) {
        String jobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
        writeResult(request, downloadJob, idsFile, contentType);
        updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
    }

    @Override
    protected StoreRequest getStoreRequest(DownloadRequest request) {
        return StoreRequest.builder().addLineage(false).build();
    }

    @Override
    protected Stream<String> streamIds(DownloadRequest request) {
        return service.streamIds(request);
    }

    @Override
    protected long getNoOfEntries(DownloadRequest downloadRequest) {
        return service.getNumberOfEntries(downloadRequest);
    }

    @Override
    protected String getDataType() {
        return UNIREF_DATA_TYPE;
    }
}
