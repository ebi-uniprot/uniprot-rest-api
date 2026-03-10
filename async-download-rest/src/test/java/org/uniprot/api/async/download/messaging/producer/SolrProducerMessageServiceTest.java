package org.uniprot.api.async.download.messaging.producer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.uniprot.api.rest.output.UniProtMediaType.EXTENDED_FASTA_MEDIA_TYPE_VALUE;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;

public abstract class SolrProducerMessageServiceTest<
                T extends SolrStreamDownloadRequest, R extends DownloadJob>
        extends ProducerMessageServiceTest<T, R> {

    @Test
    void sendMessage_forWrongTypeWithFastaX() {
        when(downloadRequest.getFormat()).thenReturn(EXTENDED_FASTA_MEDIA_TYPE_VALUE);
        lenient().when(downloadRequest.getQuery()).thenReturn("taxonomy:12345");

        assertThrows(
                IllegalArgumentException.class,
                () -> producerMessageService.sendMessage(downloadRequest));
        verify(messagingService, never()).send(any());
    }
}
