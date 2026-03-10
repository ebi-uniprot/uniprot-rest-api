package org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;

@ExtendWith(MockitoExtension.class)
class UniParcRequestProcessorWrapperTest {
    @Mock private UniParcLightSolrIdResultRequestProcessor uniParcLightSolrIdResultRequestProcessor;
    @Mock private UniParcSolrIdResultRequestProcessor uniParcSolrIdResultRequestProcessor;
    @InjectMocks private UniParcRequestProcessorWrapper uniParcRequestProcessorWrapper;
    @Mock private UniParcDownloadRequest request;

    @Test
    void process() {
        when(request.getFormat()).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        uniParcRequestProcessorWrapper.process(request);

        verify(uniParcLightSolrIdResultRequestProcessor).process(request);
    }

    @Test
    void process_xml() {
        when(request.getFormat()).thenReturn(MediaType.APPLICATION_XML_VALUE);

        uniParcRequestProcessorWrapper.process(request);

        verify(uniParcSolrIdResultRequestProcessor).process(request);
    }

    @Test
    void process_FastaX() {
        when(request.getFormat()).thenReturn(UniProtMediaType.EXTENDED_FASTA_MEDIA_TYPE_VALUE);

        uniParcRequestProcessorWrapper.process(request);

        verify(uniParcSolrIdResultRequestProcessor).process(request);
    }
}
