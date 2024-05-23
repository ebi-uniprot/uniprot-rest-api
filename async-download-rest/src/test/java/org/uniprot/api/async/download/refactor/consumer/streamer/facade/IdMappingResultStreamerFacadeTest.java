package org.uniprot.api.async.download.refactor.consumer.streamer.facade;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.idmapping.IdMappingListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping.IdMappingRDFStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingServiceUtils;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;

public abstract class IdMappingResultStreamerFacadeTest<Q, P extends EntryPair<Q>> {
    private static final String JOB_ID = "someJobId";
    private static final String FROM_1 = "from1";
    private static final String TO_1 = "to1";
    private static final String FROM_2 = "from2";
    private static final String TO_2 = "to2";
    private static final String FROM_3 = "from3";
    private static final String TO_3 = "to3";
    private static final List<String> toIds = List.of(TO_1, TO_2, TO_3);
    private static final List<IdMappingStringPair> pairs = List.of(IdMappingStringPair.builder().from(FROM_1).to(TO_1).build(),
            IdMappingStringPair.builder().from(FROM_2).to(TO_2).build(), IdMappingStringPair.builder().from(FROM_3).to(TO_3).build());
    private static final String FIELDS = "fields";
    protected IdMappingRDFStreamer rdfResultStreamer;
    protected IdMappingListResultStreamer listResultStreamer;
    protected IdMappingBatchResultStreamer<Q, P> idMappingBatchResultStreamer;
    protected MessageConverterContextFactory<P> converterContextFactory;
    protected IdMappingJobCacheService idMappingJobCacheService;
    protected IdMappingResultStreamerFacade<Q, P> idMappingResultStreamerFacade;
    protected Stream<P> entryStream;
    private MockedStatic<IdMappingServiceUtils> IdMappingUtilStatic;
    @Mock
    private IdMappingDownloadRequest downloadRequest;
    @Mock
    private MessageConverterContext<P> messageConverterContext;
    @Mock
    private IdMappingJob idMappingJob;
    @Mock
    protected IdMappingResult idMappingResult;
    @Mock
    protected List<ProblemPair> warnings;
    @Mock
    private Stream<String> rdfStream;
    @Mock
    private Stream<String> listStream;
    @Mock
    private ExtraOptions extraOptions;


    protected void mock() {
        when(downloadRequest.getJobId()).thenReturn(JOB_ID);
        when(idMappingJobCacheService.getJobAsResource(JOB_ID)).thenReturn(idMappingJob);
        when(idMappingJob.getIdMappingResult()).thenReturn(idMappingResult);
        when(idMappingResult.getMappedIds()).thenReturn(List.of(IdMappingStringPair.builder().from(FROM_1).to(TO_1).build(),
                IdMappingStringPair.builder().from(FROM_2).to(TO_2).build(), IdMappingStringPair.builder().from(FROM_3).to(TO_3).build()));
        when(downloadRequest.getFields()).thenReturn(FIELDS);
        when(idMappingResult.getWarnings()).thenReturn(warnings);
        IdMappingUtilStatic = mockStatic(IdMappingServiceUtils.class);
        IdMappingUtilStatic.when(() -> IdMappingServiceUtils.getExtraOptions(idMappingResult)).thenReturn(extraOptions);
    }

    @AfterEach
    void afterEach() {
        IdMappingUtilStatic.reset();
        IdMappingUtilStatic.close();
    }

    @Test
    void getConvertedResult_rdfType() {
        when(downloadRequest.getFormat()).thenReturn("application/rdf+xml");
        when(converterContextFactory.get(idMappingResultStreamerFacade.getResource(), RDF_MEDIA_TYPE))
                .thenReturn(messageConverterContext);
        when(rdfResultStreamer.stream(eq(downloadRequest), argThat(stream -> {
            List<String> list = stream.toList();
            List<String> toIds = IdMappingResultStreamerFacadeTest.toIds;
            return list.containsAll(toIds) && list.size() == toIds.size();
        }))).thenReturn(rdfStream);

        idMappingResultStreamerFacade.getConvertedResult(downloadRequest);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(RDF_MEDIA_TYPE);
        verify(messageConverterContext).setEntityIds(rdfStream);
        verify(messageConverterContext).setWarnings(warnings);
        verify(messageConverterContext).setExtraOptions(extraOptions);
    }

    @Test
    void getConvertedResult_ListType() {
        when(downloadRequest.getFormat()).thenReturn("text/plain;format=list");
        when(converterContextFactory.get(idMappingResultStreamerFacade.getResource(), LIST_MEDIA_TYPE))
                .thenReturn(messageConverterContext);
        when(listResultStreamer.stream(eq(downloadRequest), argThat(stream -> {
            List<String> list = stream.toList();
            List<String> toIds = IdMappingResultStreamerFacadeTest.toIds;
            return list.containsAll(toIds) && list.size() == toIds.size();
        }))).thenReturn(listStream);

        idMappingResultStreamerFacade.getConvertedResult(downloadRequest);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(LIST_MEDIA_TYPE);
        verify(messageConverterContext).setEntityIds(listStream);
        verify(messageConverterContext).setWarnings(warnings);
        verify(messageConverterContext).setExtraOptions(extraOptions);
    }

    @Test
    void getConvertedResult() {
        when(downloadRequest.getFormat()).thenReturn("application/json");
        when(converterContextFactory.get(idMappingResultStreamerFacade.getResource(), APPLICATION_JSON))
                .thenReturn(messageConverterContext);
        when(idMappingBatchResultStreamer.stream(eq(downloadRequest), argThat(stream -> {
            List<IdMappingStringPair> list = stream.toList();
            List<String> toIds = IdMappingResultStreamerFacadeTest.toIds;
            return list.containsAll(pairs) && list.size() == toIds.size();
        }))).thenReturn(entryStream);

        idMappingResultStreamerFacade.getConvertedResult(downloadRequest);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(APPLICATION_JSON);
        verify(messageConverterContext).setEntities(entryStream);
        verify(messageConverterContext).setWarnings(warnings);
        verify(messageConverterContext).setExtraOptions(extraOptions);
    }
}