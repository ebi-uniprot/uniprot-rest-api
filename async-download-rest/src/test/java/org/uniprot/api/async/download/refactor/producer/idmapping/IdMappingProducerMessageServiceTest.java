package org.uniprot.api.async.download.refactor.producer.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.idmapping.IdMappingAsyncDownloadSubmissionRules;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.messaging.idmapping.IdMappingMessagingService;
import org.uniprot.api.async.download.refactor.producer.ProducerMessageService;
import org.uniprot.api.async.download.refactor.producer.ProducerMessageServiceTest;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.rest.request.HashGenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.download.model.JobStatus.NEW;

@ExtendWith(MockitoExtension.class)
class IdMappingProducerMessageServiceTest
        extends ProducerMessageServiceTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    public static final String ID_MAPPING_FIELDS = "idMappingFields";
    public static final String ID_MAPPING_FORMAT = "idMappingFormat";
    @Mock private IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock private IdMappingJobService idMappingJobService;
    @Mock private HashGenerator<IdMappingDownloadRequest> idMappingDownloadRequestHashGenerator;
    @Mock private MessageConverter idMappingMessageConverter;
    @Mock private IdMappingMessagingService idMappingMessagingService;
    @Mock private IdMappingAsyncDownloadFileHandler idMappingAsyncDownloadFileHandler;
    @Mock private IdMappingAsyncDownloadSubmissionRules idMappingAsyncDownloadSubmissionRules;

    @BeforeEach
    void setUp() {
        this.downloadRequest = idMappingDownloadRequest;
        this.jobService = idMappingJobService;
        this.hashGenerator = idMappingDownloadRequestHashGenerator;
        this.messageConverter = idMappingMessageConverter;
        this.messagingService = idMappingMessagingService;
        this.asyncDownloadFileHandler = idMappingAsyncDownloadFileHandler;
        this.asyncDownloadSubmissionRules = idMappingAsyncDownloadSubmissionRules;

        this.producerMessageService =
                new IdMappingProducerMessageService(
                        idMappingJobService,
                        idMappingMessageConverter,
                        idMappingMessagingService,
                        idMappingDownloadRequestHashGenerator,
                        idMappingAsyncDownloadFileHandler,
                        idMappingAsyncDownloadSubmissionRules);
    }

    @Override
    protected void verifyPreprocess(IdMappingDownloadRequest downloadRequest) {}

    @Override
    protected void mockDownloadRequest() {
        mockDownloadRequestWithoutFormat();
        when(idMappingDownloadRequest.getFormat()).thenReturn(ID_MAPPING_FORMAT);
    }

    @Override
    protected void mockDownloadRequestWithoutFormat() {
        when(idMappingMessageConverter.toMessage(
                        same(downloadRequest),
                        argThat(mh -> JOB_ID.equals(mh.getHeader(ProducerMessageService.JOB_ID)))))
                .thenReturn(message);
        when(idMappingDownloadRequest.getFields()).thenReturn(ID_MAPPING_FIELDS);
    }

    @Override
    protected void verifyDownloadJob(IdMappingDownloadRequest request) {
        verify(jobService)
                .save(
                        argThat(
                                job -> {
                                    assertSame(JOB_ID, job.getId());
                                    assertSame(NEW, job.getStatus());
                                    assertNotNull(job.getCreated());
                                    assertNotNull(job.getUpdated());
                                    assertSame(request.getFields(), job.getFields());
                                    assertSame(request.getFormat(), job.getFormat());
                                    return true;
                                }));
    }
}