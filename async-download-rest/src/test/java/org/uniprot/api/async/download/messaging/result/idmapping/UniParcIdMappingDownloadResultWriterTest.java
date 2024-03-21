package org.uniprot.api.async.download.messaging.result.idmapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jodah.failsafe.RetryPolicy;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequestImpl;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UniParcIdMappingDownloadResultWriterTest {
    public static final String JOB_ID = "UNIPARC_WRITER_JOB_ID";
    public static final long PROCESSED_ENTRIES = 7L;
    private DownloadJob downloadJob;
    private IdMappingHeartbeatProducer heartBeatProducer;

    @BeforeEach
    void setUp() {
        downloadJob = mock(DownloadJob.class);
        heartBeatProducer = mock(IdMappingHeartbeatProducer.class);
        when(downloadJob.getId()).thenReturn(JOB_ID);
        when(downloadJob.getProcessedEntries()).thenReturn(PROCESSED_ENTRIES);
    }

    @Test
    void getBatchStoreEntryPairIterable() {
        RequestMappingHandlerAdapter contentAdaptor =
                Mockito.mock(RequestMappingHandlerAdapter.class);
        Mockito.when(contentAdaptor.getMessageConverters()).thenReturn(List.of());

        MessageConverterContextFactory<UniParcEntryPair> converterContextFactory = null;
        StoreStreamerConfig<UniParcEntry> storeStreamConfig =
                (StoreStreamerConfig<UniParcEntry>) Mockito.mock(StoreStreamerConfig.class);
        StreamerConfigProperties configProperties = new StreamerConfigProperties();
        Mockito.when(storeStreamConfig.getStreamConfig()).thenReturn(configProperties);

        DownloadConfigProperties downloadProperties = null;
        RdfStreamer rdfStream = null;
        UniParcIdMappingDownloadResultWriter writer =
                new UniParcIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        rdfStream,
                        heartBeatProducer);
        Iterator<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("UPI000000000B").build())
                        .iterator();
        String fields = "upi";
        BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry> result =
                writer.getBatchStoreEntryPairIterable(mappedIds, fields);
        assertNotNull(result);
    }

    @Test
    void canWriteResultFile() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        RequestMappingHandlerAdapter contentAdaptor =
                getMockedRequestMappingHandlerAdapter(objectMapper);
        MessageConverterContextFactory<UniParcEntryPair> converterContextFactory =
                getMockedMessageConverterContextFactory();

        VoldemortInMemoryUniParcEntryStore voldemortClient =
                VoldemortInMemoryUniParcEntryStore.getInstance("uniparc");
        UniParcEntry storedEntry = UniParcEntryMocker.createEntry(10, "UPI0000283A");
        voldemortClient.saveEntry(storedEntry);

        StoreStreamerConfig<UniParcEntry> storeStreamConfig =
                getMockedStoreStreamConfig(voldemortClient);

        DownloadConfigProperties downloadProperties = Mockito.mock(DownloadConfigProperties.class);
        Mockito.when(downloadProperties.getResultFilesFolder()).thenReturn("target");
        Mockito.when(downloadProperties.getIdFilesFolder()).thenReturn("target");

        UniParcIdMappingDownloadResultWriter writer =
                new UniParcIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        null,
                        heartBeatProducer);
        List<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("UPI0000283A10").build());
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFields("upi");
        request.setFormat("json");
        request.setJobId(JOB_ID);

        ProblemPair warning = new ProblemPair(1, "msg");
        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedIds(mappedIds)
                        .warning(warning)
                        .unmappedId("unm1")
                        .suggestedId(new IdMappingStringPair("fromSug1", "toSug1"))
                        .obsoleteCount(1)
                        .build();

        assertDoesNotThrow(
                () ->
                        writer.writeResult(
                                request, idMappingResult, downloadJob, MediaType.APPLICATION_JSON));

        Path resultFilePath =
                Path.of("target" + File.separator + JOB_ID + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                objectMapper.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(1, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P12345")));

        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("uniParcId"))
                        .map(node -> node.findValue("value").asText())
                        .collect(Collectors.toSet())
                        .contains("UPI0000283A10"));

        ArrayNode failedIds = (ArrayNode) jsonResult.findPath("failedIds");
        assertEquals(1, failedIds.size());
        assertEquals("unm1", failedIds.get(0).asText());

        ArrayNode suggestedIds = (ArrayNode) jsonResult.findPath("suggestedIds");
        assertEquals(1, suggestedIds.size());
        JsonNode suggestNode = suggestedIds.get(0);
        assertEquals("fromSug1", suggestNode.findValue("from").asText());
        assertEquals("toSug1", suggestNode.findValue("to").asText());

        JsonNode obsoleteCountNode = (JsonNode) jsonResult.findPath("obsoleteCount");
        int obsoleteCount = obsoleteCountNode.asInt();
        assertEquals(1, obsoleteCount);
        ArrayNode warnings = (ArrayNode) jsonResult.findPath("warnings");
        assertEquals(1, warnings.size());
        JsonNode warningNode = warnings.get(0);
        assertEquals("1", warningNode.findValue("code").asText());
        assertEquals("msg", warningNode.findValue("message").asText());

        verify(heartBeatProducer, atLeastOnce()).createForResults(same(downloadJob), anyLong());
        verify(heartBeatProducer).stop(JOB_ID);
    }

    @Test
    void canGetType() {
        RequestMappingHandlerAdapter contentAdaptor =
                Mockito.mock(RequestMappingHandlerAdapter.class);
        Mockito.when(contentAdaptor.getMessageConverters()).thenReturn(List.of());

        MessageConverterContextFactory<UniParcEntryPair> converterContextFactory = null;
        StoreStreamerConfig<UniParcEntry> storeStreamConfig = null;
        DownloadConfigProperties downloadProperties = null;
        RdfStreamer rdfStream = null;
        UniParcIdMappingDownloadResultWriter writer =
                new UniParcIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        rdfStream,
                        heartBeatProducer);
        Type result = writer.getType();
        assertNotNull(result);
        assertEquals(
                "org.uniprot.api.rest.output.context.MessageConverterContext<org.uniprot.api.idmapping.common.response.model.UniParcEntryPair>",
                result.getTypeName());
    }

    private MessageConverterContextFactory<UniParcEntryPair>
            getMockedMessageConverterContextFactory() {
        MessageConverterContext<UniParcEntryPair> context =
                MessageConverterContext.<UniParcEntryPair>builder()
                        .entities(Stream.of(UniParcEntryPair.builder().build()))
                        .fields("upi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .build();

        MessageConverterContextFactory<UniParcEntryPair> converterContextFactory =
                Mockito.mock(MessageConverterContextFactory.class);
        Mockito.when(
                        converterContextFactory.get(
                                Mockito.any(), Mockito.eq(MediaType.APPLICATION_JSON)))
                .thenReturn(context);
        return converterContextFactory;
    }

    private RequestMappingHandlerAdapter getMockedRequestMappingHandlerAdapter(
            ObjectMapper mapper) {
        RequestMappingHandlerAdapter contentAdaptor =
                Mockito.mock(RequestMappingHandlerAdapter.class);
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC);
        JsonMessageConverter<UniParcEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(mapper, UniParcEntryPair.class, returnFieldConfig, null);
        Mockito.when(contentAdaptor.getMessageConverters())
                .thenReturn(List.of(jsonMessageConverter));
        return contentAdaptor;
    }

    private StoreStreamerConfig<UniParcEntry> getMockedStoreStreamConfig(
            VoldemortInMemoryUniParcEntryStore voldemortClient) {
        StreamerConfigProperties configProperties = new StreamerConfigProperties();
        configProperties.setStoreBatchSize(5);

        RetryPolicy<Object> storeRetryPolicy = new RetryPolicy<>().withMaxRetries(1);

        StoreStreamerConfig<UniParcEntry> storeStreamConfig =
                (StoreStreamerConfig<UniParcEntry>) Mockito.mock(StoreStreamerConfig.class);
        Mockito.when(storeStreamConfig.getStreamConfig()).thenReturn(configProperties);
        Mockito.when(storeStreamConfig.getStoreFetchRetryPolicy()).thenReturn(storeRetryPolicy);
        Mockito.when(storeStreamConfig.getStoreClient())
                .thenReturn(new UniProtStoreClient<>(voldemortClient));

        return storeStreamConfig;
    }
}
