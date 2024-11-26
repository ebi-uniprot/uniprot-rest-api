package org.uniprot.api.async.download.messaging.producer.uniprotkb;

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
import org.uniprot.api.async.download.messaging.producer.ProducerMessageService;
import org.uniprot.api.async.download.messaging.producer.ProducerMessageServiceTest;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.mq.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.rest.request.HashGenerator;

@ExtendWith(MockitoExtension.class)
class UniProtKBProducerMessageServiceTest
        extends ProducerMessageServiceTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public static final String UNIPROTKB_FIELDS = "uniProtKBFields";
    public static final String UNIPROTKB_SORT = "uniProtKBSort";
    public static final String UNIPROTKB_FORMAT = "uniProtKBFormat";
    protected final String UNI_PROT_KB_QUERY = "uniProtKBQuery";
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock private UniProtKBJobService uniProtKBJobService;
    @Mock private HashGenerator<UniProtKBDownloadRequest> uniProtKBDownloadRequestHashGenerator;
    @Mock private MessageConverter uniProtKBMessageConverter;
    @Mock private UniProtKBMessagingService uniProtKBMessagingService;
    @Mock private UniProtKBFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock private UniProtKBJobSubmissionRules uniProtKBAsyncDownloadSubmissionRules;

    @BeforeEach
    void setUp() {
        this.downloadRequest = uniProtKBDownloadRequest;
        this.jobService = uniProtKBJobService;
        this.hashGenerator = uniProtKBDownloadRequestHashGenerator;
        this.messageConverter = uniProtKBMessageConverter;
        this.messagingService = uniProtKBMessagingService;
        this.fileHandler = uniProtKBAsyncDownloadFileHandler;
        this.jobSubmissionRules = uniProtKBAsyncDownloadSubmissionRules;

        this.producerMessageService =
                new UniProtKBProducerMessageService(
                        uniProtKBJobService,
                        uniProtKBMessageConverter,
                        uniProtKBMessagingService,
                        uniProtKBDownloadRequestHashGenerator,
                        uniProtKBAsyncDownloadFileHandler,
                        uniProtKBAsyncDownloadSubmissionRules);
    }

    @Override
    protected void verifyPreprocess(UniProtKBDownloadRequest downloadRequest) {
        verify(downloadRequest).setLargeSolrStreamRestricted(false);
    }

    @Override
    protected void mockDownloadRequest() {
        mockDownloadRequestWithoutFormat();
        when(uniProtKBDownloadRequest.getFormat()).thenReturn(UNIPROTKB_FORMAT);
    }

    @Override
    protected void mockDownloadRequestWithoutFormat() {
        when(uniProtKBMessageConverter.toMessage(
                        same(downloadRequest),
                        argThat(mh -> JOB_ID.equals(mh.getHeader(ProducerMessageService.JOB_ID)))))
                .thenReturn(message);
        when(uniProtKBDownloadRequest.getQuery()).thenReturn(UNI_PROT_KB_QUERY);
        when(uniProtKBDownloadRequest.getFields()).thenReturn(UNIPROTKB_FIELDS);
        when(uniProtKBDownloadRequest.getSort()).thenReturn(UNIPROTKB_SORT);
    }

    @Override
    protected void verifyDownloadJob(UniProtKBDownloadRequest request) {
        verify(jobService)
                .create(
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
