package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.List;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service("Consumer")
@Slf4j
public class UniProtKBMessageListener implements MessageListener {

    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;
    private final List<HttpMessageConverter<?>> messageConverters;

    private DownloadJobRepository jobRepository;

    public UniProtKBMessageListener(
            MessageConverter converter,
            UniProtEntryService service,
            DownloadConfigProperties downloadConfigProperties,
            DownloadJobRepository jobRepository,
            RequestMappingHandlerAdapter contentAdapter) {
        this.converter = converter;
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
        this.jobRepository = jobRepository;
        this.messageConverters = contentAdapter.getMessageConverters();
    }

    @Override
    public void onMessage(Message message) {
        UniProtKBStreamRequest request = (UniProtKBStreamRequest) converter.fromMessage(message);
        String jobId = message.getMessageProperties().getHeader("jobId");
        Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
        if (optDownloadJob.isEmpty()) {
            // TODO handle error
        }
        DownloadJob downloadJob = optDownloadJob.get();
        downloadJob.setStatus(JobStatus.RUNNING);
        String contentType = message.getMessageProperties().getHeader("content-type");

        if(contentType == null){ //TODO: REMOVE IT
            contentType = "application/json";
        }

        Path idsFile = Paths.get(downloadConfigProperties.getFolder(), jobId);
        if (Files.notExists(idsFile)) {
            updateDownloadJob(downloadJob, JobStatus.RUNNING);
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            updateDownloadJob(downloadJob, JobStatus.FINISHED);
        } else {
            // redis update status?
        }

        Type type = (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntry>>() {
        }).getType();
        AbstractUUWHttpMessageConverter outputWriter = getOutputWriter(contentType, type);
        //outputWriter.writeContents(context, new File);

        // TESTING TO ALSO CREATE RESULT
        log.info("Message processed");

        // acknowledge the queue with failure/success
    }

    private AbstractUUWHttpMessageConverter getOutputWriter(String contentType, Type type) {
        MediaType mediaType = UniProtMediaType.valueOf(contentType);

        return (AbstractUUWHttpMessageConverter) messageConverters.stream()
                .filter(c -> c instanceof AbstractUUWHttpMessageConverter)
                .filter(c -> ((GenericHttpMessageConverter<?>) c).canWrite(type, MessageConverterContext.class, mediaType))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find "));
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) {
        Iterable<String> source = ids::iterator;
        try {
            Files.write(filePath, source, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Stream<String> streamIds(UniProtKBStreamRequest request) {
        return service.streamIds(request);
    }

    private void updateDownloadJob(DownloadJob downloadJob, JobStatus jobStatus) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        downloadJob.setUpdated(now);
        downloadJob.setStatus(jobStatus);
        this.jobRepository.save(downloadJob);
    }
}
