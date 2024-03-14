package org.uniprot.api.async.download.messaging.listener.uniref;

import java.nio.file.Path;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;
import org.uniprot.api.async.download.messaging.listener.common.AbstractMessageListener;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefDownloadResultWriter;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadRequest;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

/**
 * @author tibrahim
 * @created 16/08/2023
 */
@Service
@Slf4j
public class UniRefMessageListener extends AbstractMessageListener implements MessageListener {
    private static final String UNIREF_DATA_TYPE = "uniref";
    private final UniRefEntryLightService service;

    public UniRefMessageListener(
            MessageConverter converter,
            UniRefDownloadConfigProperties uniRefDownloadConfigProperties,
            UniRefAsyncDownloadQueueConfigProperties queueConfigProperties,
            DownloadJobRepository jobRepository,
            UniRefDownloadResultWriter uniRefDownloadResultWriter,
            UniRefRabbitTemplate uniRefRabbitTemplate,
            UniRefEntryLightService service,
            HeartbeatProducer heartbeatProducer,
            UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler) {
        super(
                converter,
                uniRefDownloadConfigProperties,
                jobRepository,
                uniRefDownloadResultWriter,
                uniRefRabbitTemplate,
                heartbeatProducer,
                uniRefAsyncDownloadFileHandler,
                queueConfigProperties);
        this.service = service;
    }

    @Override
    protected void updateStatusAndWriteResult(
            Message message, DownloadJob downloadJob, DownloadRequest request, Path idsFile) {
        String jobId = downloadJob.getId();
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        updateDownloadJob(message, downloadJob, JobStatus.RUNNING);
        Long totalHits = getSolrHits((UniRefDownloadRequest) request);
        updateTotalEntries(downloadJob, totalHits);
        writeResult(request, downloadJob, idsFile, contentType);
        updateDownloadJob(message, downloadJob, JobStatus.FINISHED, jobId);
    }

    @Override
    protected StoreRequest getStoreRequest(DownloadRequest request) {
        return StoreRequest.builder().addLineage(false).build();
    }

    private Long getSolrHits(UniRefDownloadRequest request) {
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setQuery(request.getQuery());
        searchRequest.setSize(0);
        QueryResult<UniRefEntryLight> searchResults = service.search(searchRequest);
        return searchResults.getPage().getTotalElements();
    }

    @Override
    public Stream<String> streamIds(DownloadRequest request) {
        return service.streamIds(request);
    }

    @Override
    protected String getDataType() {
        return UNIREF_DATA_TYPE;
    }
}
