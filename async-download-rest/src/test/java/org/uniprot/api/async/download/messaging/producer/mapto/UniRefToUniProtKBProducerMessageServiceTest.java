package org.uniprot.api.async.download.messaging.producer.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.ProducerMessageService;
import org.uniprot.api.async.download.model.request.mapto.UniRefToUniProtKBDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.download.model.JobStatus.NEW;

@ExtendWith(MockitoExtension.class)
public class UniRefToUniProtKBProducerMessageServiceTest
        extends MapToProducerMessageServiceTest<UniRefToUniProtKBDownloadRequest> {
    public static final String UNIREF_MAP_FIELDS = "uniRefMapFields";
    public static final String UNIREF_MAP_SORT = "uniRefMapSort";
    public static final String UNIREF_MAP_FORMAT = "uniRefMapFormat";
    protected final String UNIREF_MAP_QUERY = "uniRefMapQuery";
    @Mock private UniRefToUniProtKBDownloadRequest mapDownloadRequest;

    @Mock
    private HashGenerator<UniRefToUniProtKBDownloadRequest>
            uniProtKBMapDownloadRequestHashGenerator;

    @Mock
    private UniRefToUniProtKBJobSubmissionRules
            uniRefToUniProtKBJobSubmissionRules;

    @BeforeEach
    void setUp() {
        init();
        this.downloadRequest = mapDownloadRequest;
        this.hashGenerator = uniProtKBMapDownloadRequestHashGenerator;
        this.jobSubmissionRules = uniRefToUniProtKBJobSubmissionRules;
        this.producerMessageService =
                new UniRefToUniProtKBProducerMessageService(
                        mapToJobService,
                        mapMessageConverter,
                        mapToMessagingService,
                        hashGenerator,
                        mapDownloadFileHandler,
                        uniRefToUniProtKBJobSubmissionRules);
    }

    @Override
    protected void mockDownloadRequest() {
        mockDownloadRequestWithoutFormat();
        when(downloadRequest.getFormat()).thenReturn(UNIREF_MAP_FORMAT);
    }

    @Override
    protected void mockDownloadRequestWithoutFormat() {
        when(messageConverter.toMessage(
                        same(downloadRequest),
                        argThat(mh -> JOB_ID.equals(mh.getHeader(ProducerMessageService.JOB_ID)))))
                .thenReturn(message);
        when(downloadRequest.getQuery()).thenReturn(UNIREF_MAP_QUERY);
        when(downloadRequest.getFields()).thenReturn(UNIREF_MAP_FIELDS);
        when(downloadRequest.getSort()).thenReturn(UNIREF_MAP_SORT);
    }

    @Override
    protected void verifyDownloadJob(UniRefToUniProtKBDownloadRequest request) {
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
