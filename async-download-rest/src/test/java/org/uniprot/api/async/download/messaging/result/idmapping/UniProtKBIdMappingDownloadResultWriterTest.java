package org.uniprot.api.async.download.messaging.result.idmapping;

import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequestImpl;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class UniProtKBIdMappingDownloadResultWriterTest {
    public static final String JOB_ID = "UNIPROTKB_WRITER_JOB_ID";
    public static final long PROCESSED_ENTRIES = 39L;
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
        RequestMappingHandlerAdapter contentAdaptor = mock(RequestMappingHandlerAdapter.class);
        when(contentAdaptor.getMessageConverters()).thenReturn(List.of());

        MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory = null;
        StoreStreamerConfig<UniProtKBEntry> storeStreamConfig =
                (StoreStreamerConfig<UniProtKBEntry>) mock(StoreStreamerConfig.class);
        StreamerConfigProperties configProperties = new StreamerConfigProperties();
        when(storeStreamConfig.getStreamConfig()).thenReturn(configProperties);

        DownloadConfigProperties downloadProperties = null;
        RdfStreamer rdfStream = null;
        UniProtKBIdMappingDownloadResultWriter writer =
                new UniProtKBIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        rdfStream,
                        null,
                        null,
                        heartBeatProducer);
        Iterator<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .iterator();
        String fields = "gene_names";
        BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry> result =
                writer.getBatchStoreEntryPairIterable(mappedIds, fields);
        assertNotNull(result);
    }

    @Test
    void canWriteResultFile() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        RequestMappingHandlerAdapter contentAdaptor =
                getMockedRequestMappingHandlerAdapter(objectMapper);
        MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory =
                getMockedMessageConverterContextFactory();

        VoldemortInMemoryUniprotEntryStore voldemortClient =
                VoldemortInMemoryUniprotEntryStore.getInstance("uniprot");
        UniProtKBEntry storedEntry = UniProtEntryMocker.create("P12345");
        voldemortClient.saveEntry(storedEntry);

        StoreStreamerConfig<UniProtKBEntry> storeStreamConfig =
                getMockedStoreStreamConfig(voldemortClient);

        DownloadConfigProperties downloadProperties = mock(DownloadConfigProperties.class);
        when(downloadProperties.getResultFilesFolder()).thenReturn("target");
        UniprotKBMappingRepository uniprotKBMappingRepository =
                mock(UniprotKBMappingRepository.class);

        UniProtKBIdMappingDownloadResultWriter writer =
                new UniProtKBIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        null,
                        null,
                        uniprotKBMappingRepository,
                        heartBeatProducer);
        List<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("P12345").build());

        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFields("gene_names");
        request.setFormat("json");
        request.setJobId(JOB_ID);

        IdMappingResult idMappingResult = IdMappingResult.builder().mappedIds(mappedIds).build();

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
        assertTrue(results.findValuesAsText("from").contains("P12345"));

        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("primaryAccession"))
                        .map(node -> node.findValue("value").asText())
                        .collect(Collectors.toSet())
                        .contains("P12345"));

        verify(heartBeatProducer, atLeastOnce()).createForResults(same(downloadJob), anyLong());
        verify(heartBeatProducer).stop(JOB_ID);
    }

    @Test
    void canGetType() {
        RequestMappingHandlerAdapter contentAdaptor = mock(RequestMappingHandlerAdapter.class);
        when(contentAdaptor.getMessageConverters()).thenReturn(List.of());

        MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory = null;
        StoreStreamerConfig<UniProtKBEntry> storeStreamConfig = null;
        DownloadConfigProperties downloadProperties = null;
        RdfStreamer rdfStream = null;
        UniProtKBIdMappingDownloadResultWriter writer =
                new UniProtKBIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        rdfStream,
                        null,
                        null,
                        heartBeatProducer);
        Type result = writer.getType();
        assertNotNull(result);
        assertEquals(
                "org.uniprot.api.rest.output.context.MessageConverterContext<org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair>",
                result.getTypeName());
    }

    private MessageConverterContextFactory<UniProtKBEntryPair>
            getMockedMessageConverterContextFactory() {
        MessageConverterContext<UniProtKBEntryPair> context =
                MessageConverterContext.<UniProtKBEntryPair>builder()
                        .entities(Stream.of(UniProtKBEntryPair.builder().build()))
                        .fields("upi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .build();

        MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory =
                mock(MessageConverterContextFactory.class);
        when(converterContextFactory.get(any(), eq(MediaType.APPLICATION_JSON)))
                .thenReturn(context);
        return converterContextFactory;
    }

    private RequestMappingHandlerAdapter getMockedRequestMappingHandlerAdapter(
            ObjectMapper mapper) {
        RequestMappingHandlerAdapter contentAdaptor = mock(RequestMappingHandlerAdapter.class);
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        JsonMessageConverter<UniProtKBEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(
                        mapper, UniProtKBEntryPair.class, returnFieldConfig, null);
        when(contentAdaptor.getMessageConverters()).thenReturn(List.of(jsonMessageConverter));
        return contentAdaptor;
    }

    private StoreStreamerConfig<UniProtKBEntry> getMockedStoreStreamConfig(
            VoldemortInMemoryUniprotEntryStore voldemortClient) {
        StreamerConfigProperties configProperties = new StreamerConfigProperties();
        configProperties.setStoreBatchSize(5);

        RetryPolicy<Object> storeRetryPolicy = new RetryPolicy<>().withMaxRetries(1);

        StoreStreamerConfig<UniProtKBEntry> storeStreamConfig =
                (StoreStreamerConfig<UniProtKBEntry>) mock(StoreStreamerConfig.class);
        when(storeStreamConfig.getStreamConfig()).thenReturn(configProperties);
        when(storeStreamConfig.getStoreFetchRetryPolicy()).thenReturn(storeRetryPolicy);
        when(storeStreamConfig.getStoreClient())
                .thenReturn(new UniProtStoreClient<>(voldemortClient));

        return storeStreamConfig;
    }
}
