package org.uniprot.api.uniprotkb.queue;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

@Service("ConsumerErrorHandler")
@Slf4j
public class MessageListenerErrorHandler implements ErrorHandler {

    private final DownloadJobRepository jobRepository;
    private final DownloadConfigProperties configProperties;

    public MessageListenerErrorHandler(
            DownloadJobRepository jobRepository,
            DownloadConfigProperties downloadConfigProperties) {
        this.jobRepository = jobRepository;
        this.configProperties = downloadConfigProperties;
    }

    @Override
    public void handleError(Throwable throwable) {
        // if timeout set the job status to failed with reason
        // increase the retry count
        // what about download running job if we delete the files?
        // don't replay the message deletion of files will force replay
        // we should rethrow the exception?
        log.warn("Unhandled exception in MessageListenerErrorHandler" + throwable.getMessage());
    }
}
