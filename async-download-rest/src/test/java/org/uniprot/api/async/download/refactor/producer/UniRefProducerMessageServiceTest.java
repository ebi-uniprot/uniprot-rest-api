package org.uniprot.api.async.download.refactor.producer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.download.model.JobStatus.NEW;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.uniref.UniRefAsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.messaging.UniRefMessagingService;
import org.uniprot.api.async.download.refactor.request.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.UniRefJobService;
import org.uniprot.api.rest.request.HashGenerator;

@ExtendWith(MockitoExtension.class)
public class UniRefProducerMessageServiceTest
        extends ProducerMessageServiceTest<UniRefDownloadRequest, UniRefDownloadJob> {
    public static final String UNI_REF_FIELDS = "uniRefFields";
    public static final String UNI_REF_SORT = "uniRefSort";
    public static final String UNI_REF_FORMAT = "uniRefFormat";
    protected final String UNI_REF_QUERY = "uniRefQuery";
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private UniRefJobService uniRefJobService;
    @Mock private HashGenerator<UniRefDownloadRequest> uniRefDownloadRequestHashGenerator;
    @Mock private MessageConverter uniRefMessageConverter;
    @Mock private UniRefMessagingService uniRefMessagingService;
    @Mock private UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler;
    @Mock private UniRefAsyncDownloadSubmissionRules uniRefAsyncDownloadSubmissionRules;

    @BeforeEach
    void setUp() {
        this.downloadRequest = uniRefDownloadRequest;
        this.jobService = uniRefJobService;
        this.hashGenerator = uniRefDownloadRequestHashGenerator;
        this.messageConverter = uniRefMessageConverter;
        this.messagingService = uniRefMessagingService;
        this.asyncDownloadFileHandler = uniRefAsyncDownloadFileHandler;
        this.asyncDownloadSubmissionRules = uniRefAsyncDownloadSubmissionRules;

        this.producerMessageService =
                new UniRefProducerMessageService(
                        uniRefJobService,
                        uniRefMessageConverter,
                        uniRefMessagingService,
                        uniRefDownloadRequestHashGenerator,
                        uniRefAsyncDownloadFileHandler,
                        uniRefAsyncDownloadSubmissionRules);
    }

    @Override
    protected void verifyPreprocess(UniRefDownloadRequest downloadRequest) {
        verify(downloadRequest).setLargeSolrStreamRestricted(false);
    }

    @Override
    protected void mockDownloadRequest() {
        mockDownloadRequestWithoutFormat();
        when(uniRefDownloadRequest.getFormat()).thenReturn(UNI_REF_FORMAT);
    }

    @Override
    protected void mockDownloadRequestWithoutFormat() {
        when(uniRefMessageConverter.toMessage(
                        same(downloadRequest),
                        argThat(mh -> JOB_ID.equals(mh.getHeader(ProducerMessageService.JOB_ID)))))
                .thenReturn(message);
        when(uniRefDownloadRequest.getQuery()).thenReturn(UNI_REF_QUERY);
        when(uniRefDownloadRequest.getFields()).thenReturn(UNI_REF_FIELDS);
        when(uniRefDownloadRequest.getSort()).thenReturn(UNI_REF_SORT);
    }

    @Override
    protected void verifyDownloadJob(UniRefDownloadRequest request) {
        verify(jobService)
                .save(
                        argThat(
                                job -> {
                                    assertSame(JOB_ID, job.getId());
                                    assertSame(NEW, job.getStatus());
                                    assertNotNull(job.getCreated());
                                    assertNotNull(job.getUpdated());
                                    assertSame(request.getQuery(), job.getQuery());
                                    assertSame(request.getFields(), job.getFields());
                                    assertSame(request.getSort(), job.getSort());
                                    assertSame(request.getFormat(), job.getFormat());
                                    return true;
                                }));
    }
}
