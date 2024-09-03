package org.uniprot.api.async.download.messaging.producer.map;

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
import org.uniprot.api.async.download.messaging.producer.ProducerMessageService;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

@ExtendWith(MockitoExtension.class)
public class UniProtKBMapProducerMessageServiceTest
        extends MapProducerMessageServiceTest<UniProtKBToUniRefMapDownloadRequest> {
    public static final String UNIPROTKB_MAP_FIELDS = "uniProtKBMapFields";
    public static final String UNIPROTKB_MAP_SORT = "uniProtKBMapSort";
    public static final String UNIPROTKB_MAP_FORMAT = "uniProtKBMapFormat";
    protected final String UNIPROTKB_MAP_QUERY = "uniProtKBMapQuery";
    @Mock private UniProtKBToUniRefMapDownloadRequest mapDownloadRequest;

    @Mock
    private HashGenerator<UniProtKBToUniRefMapDownloadRequest>
            uniProtKBMapDownloadRequestHashGenerator;

    @Mock
    private MapJobSubmissionRules<UniProtKBToUniRefMapDownloadRequest>
            uniProtKBMapDownloadRequestMapJobSubmissionRules;

    @BeforeEach
    void setUp() {
        init();
        this.downloadRequest = mapDownloadRequest;
        this.hashGenerator = uniProtKBMapDownloadRequestHashGenerator;
        this.jobSubmissionRules = uniProtKBMapDownloadRequestMapJobSubmissionRules;
        this.producerMessageService =
                new UniProtKBMapProducerMessageService(
                        mapJobService,
                        mapMessageConverter,
                        mapMessagingService,
                        hashGenerator,
                        mapDownloadFileHandler,
                        uniProtKBMapDownloadRequestMapJobSubmissionRules);
    }

    @Override
    protected void mockDownloadRequest() {
        mockDownloadRequestWithoutFormat();
        when(downloadRequest.getFormat()).thenReturn(UNIPROTKB_MAP_FORMAT);
    }

    @Override
    protected void mockDownloadRequestWithoutFormat() {
        when(messageConverter.toMessage(
                        same(downloadRequest),
                        argThat(mh -> JOB_ID.equals(mh.getHeader(ProducerMessageService.JOB_ID)))))
                .thenReturn(message);
        when(downloadRequest.getQuery()).thenReturn(UNIPROTKB_MAP_QUERY);
        when(downloadRequest.getFields()).thenReturn(UNIPROTKB_MAP_FIELDS);
        when(downloadRequest.getSort()).thenReturn(UNIPROTKB_MAP_SORT);
    }

    @Override
    protected void verifyDownloadJob(UniProtKBToUniRefMapDownloadRequest request) {
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
