package org.uniprot.api.async.download.messaging.producer.uniparc;

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
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.messaging.service.uniparc.UniParcMessagingService;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.rest.request.HashGenerator;

@ExtendWith(MockitoExtension.class)
class UniParcProducerMessageServiceTest
        extends ProducerMessageServiceTest<UniParcDownloadRequest, UniParcDownloadJob> {
    public static final String UNIPARC_FIELDS = "uniParcFields";
    public static final String UNIPARC_SORT = "uniParcSort";
    public static final String UNIPARC_FORMAT = "uniParcFormat";
    protected final String UNIPARC_QUERY = "uniParcQuery";
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcJobService uniParcJobService;
    @Mock private HashGenerator<UniParcDownloadRequest> uniParcDownloadRequestHashGenerator;
    @Mock private MessageConverter uniParcMessageConverter;
    @Mock private UniParcMessagingService uniParcMessagingService;
    @Mock private UniParcFileHandler uniParcAsyncDownloadFileHandler;
    @Mock private UniParcJobSubmissionRules uniParcAsyncDownloadSubmissionRules;

    @BeforeEach
    void setUp() {
        this.downloadRequest = uniParcDownloadRequest;
        this.jobService = uniParcJobService;
        this.hashGenerator = uniParcDownloadRequestHashGenerator;
        this.messageConverter = uniParcMessageConverter;
        this.messagingService = uniParcMessagingService;
        this.fileHandler = uniParcAsyncDownloadFileHandler;
        this.jobSubmissionRules = uniParcAsyncDownloadSubmissionRules;

        this.producerMessageService =
                new UniParcProducerMessageService(
                        uniParcJobService,
                        uniParcMessageConverter,
                        uniParcMessagingService,
                        uniParcDownloadRequestHashGenerator,
                        uniParcAsyncDownloadFileHandler,
                        uniParcAsyncDownloadSubmissionRules);
    }

    @Override
    protected void verifyPreprocess(UniParcDownloadRequest downloadRequest) {
        verify(downloadRequest).setLargeSolrStreamRestricted(false);
    }

    @Override
    protected void mockDownloadRequest() {
        mockDownloadRequestWithoutFormat();
        when(uniParcDownloadRequest.getFormat()).thenReturn(UNIPARC_FORMAT);
    }

    @Override
    protected void mockDownloadRequestWithoutFormat() {
        when(uniParcMessageConverter.toMessage(
                        same(downloadRequest),
                        argThat(mh -> JOB_ID.equals(mh.getHeader(ProducerMessageService.JOB_ID)))))
                .thenReturn(message);
        when(uniParcDownloadRequest.getQuery()).thenReturn(UNIPARC_QUERY);
        when(uniParcDownloadRequest.getFields()).thenReturn(UNIPARC_FIELDS);
        when(uniParcDownloadRequest.getSort()).thenReturn(UNIPARC_SORT);
    }

    @Override
    protected void verifyDownloadJob(UniParcDownloadRequest request) {
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
