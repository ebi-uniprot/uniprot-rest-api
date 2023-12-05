package org.uniprot.api.idmapping.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequestImpl;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.rest.download.heartbeat.HeartBeatProducer;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniRefIdMappingDownloadResultWriterTest {
    public static final String JOB_ID = "UNIREF_WRITER_JOB_ID";
    public static final long PROCESSED_ENTRIES = 7L;
    private DownloadJob downloadJob;
    private HeartBeatProducer heartBeatProducer;

    @BeforeEach
    void setUp() {
        downloadJob = mock(DownloadJob.class);
        heartBeatProducer = mock(HeartBeatProducer.class);
        when(downloadJob.getId()).thenReturn(JOB_ID);
        when(downloadJob.getEntriesProcessed()).thenReturn(PROCESSED_ENTRIES);
    }

    @Test
    void getBatchStoreEntryPairIterable() {
        RequestMappingHandlerAdapter contentAdaptor =
                Mockito.mock(RequestMappingHandlerAdapter.class);
        Mockito.when(contentAdaptor.getMessageConverters()).thenReturn(List.of());

        MessageConverterContextFactory<UniRefEntryPair> converterContextFactory = null;
        StoreStreamerConfig<UniRefEntryLight> storeStreamConfig =
                (StoreStreamerConfig<UniRefEntryLight>) Mockito.mock(StoreStreamerConfig.class);
        StreamerConfigProperties configProperties = new StreamerConfigProperties();
        Mockito.when(storeStreamConfig.getStreamConfig()).thenReturn(configProperties);

        DownloadConfigProperties downloadProperties = null;
        RdfStreamer rdfStream = null;
        UniRefIdMappingDownloadResultWriter writer =
                new UniRefIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        rdfStream,
                        heartBeatProducer);
        Iterator<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("UPI000000000B").build())
                        .iterator();
        String fields = "name";
        BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight> result =
                writer.getBatchStoreEntryPairIterable(mappedIds, fields);
        assertNotNull(result);
    }

    @Test
    void canWriteResultFile() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RequestMappingHandlerAdapter contentAdaptor =
                getMockedRequestMappingHandlerAdapter(objectMapper);
        MessageConverterContextFactory<UniRefEntryPair> converterContextFactory =
                getMockedMessageConverterContextFactory();

        VoldemortInMemoryUniRefEntryLightStore voldemortClient =
                VoldemortInMemoryUniRefEntryLightStore.getInstance("uniref_light");
        UniRefEntryLight storedEntry = createEntry();
        voldemortClient.saveEntry(storedEntry);

        StoreStreamerConfig<UniRefEntryLight> storeStreamConfig =
                getMockedStoreStreamConfig(voldemortClient);

        DownloadConfigProperties downloadProperties = Mockito.mock(DownloadConfigProperties.class);
        Mockito.when(downloadProperties.getResultFilesFolder()).thenReturn("target");

        UniRefIdMappingDownloadResultWriter writer =
                new UniRefIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        null,
                        heartBeatProducer);
        List<IdMappingStringPair> mappedIds =
                List.of(
                        IdMappingStringPair.builder()
                                .from("P03910")
                                .to("UniRef100_P03910")
                                .build());

        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFields("name");
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
        assertTrue(results.findValuesAsText("from").contains("P03910"));

        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("id"))
                        .map(node -> node.findValue("value").asText())
                        .collect(Collectors.toSet())
                        .contains("UniRef100_P03910"));
    }

    @Test
    void canGetType() {
        RequestMappingHandlerAdapter contentAdaptor =
                Mockito.mock(RequestMappingHandlerAdapter.class);
        Mockito.when(contentAdaptor.getMessageConverters()).thenReturn(List.of());

        MessageConverterContextFactory<UniRefEntryPair> converterContextFactory = null;
        StoreStreamerConfig<UniRefEntryLight> storeStreamConfig = null;
        DownloadConfigProperties downloadProperties = null;
        RdfStreamer rdfStream = null;
        UniRefIdMappingDownloadResultWriter writer =
                new UniRefIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        rdfStream,
                        heartBeatProducer);
        Type result = writer.getType();
        assertNotNull(result);
        assertEquals(
                "org.uniprot.api.rest.output.context.MessageConverterContext<org.uniprot.api.idmapping.model.UniRefEntryPair>",
                result.getTypeName());
    }

    private MessageConverterContextFactory<UniRefEntryPair>
            getMockedMessageConverterContextFactory() {
        MessageConverterContext<UniRefEntryPair> context =
                MessageConverterContext.<UniRefEntryPair>builder()
                        .entities(Stream.of(UniRefEntryPair.builder().build()))
                        .fields("name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .build();

        MessageConverterContextFactory<UniRefEntryPair> converterContextFactory =
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
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF);
        JsonMessageConverter<UniRefEntryPair> jsonMessageConverter =
                new JsonMessageConverter<>(mapper, UniRefEntryPair.class, returnFieldConfig, null);
        Mockito.when(contentAdaptor.getMessageConverters())
                .thenReturn(List.of(jsonMessageConverter));
        return contentAdaptor;
    }

    private StoreStreamerConfig<UniRefEntryLight> getMockedStoreStreamConfig(
            VoldemortInMemoryUniRefEntryLightStore voldemortClient) {
        StreamerConfigProperties configProperties = new StreamerConfigProperties();
        configProperties.setStoreBatchSize(5);

        RetryPolicy<Object> storeRetryPolicy = new RetryPolicy<>().withMaxRetries(1);

        StoreStreamerConfig<UniRefEntryLight> storeStreamConfig =
                (StoreStreamerConfig<UniRefEntryLight>) Mockito.mock(StoreStreamerConfig.class);
        Mockito.when(storeStreamConfig.getStreamConfig()).thenReturn(configProperties);
        Mockito.when(storeStreamConfig.getStoreFetchRetryPolicy()).thenReturn(storeRetryPolicy);
        Mockito.when(storeStreamConfig.getStoreClient())
                .thenReturn(new UniProtStoreClient<>(voldemortClient));

        return storeStreamConfig;
    }

    private UniRefEntryLight createEntry() {
        UniRefEntry uniRefEntry = UniRefEntryMocker.createEntry(10, UniRefType.UniRef100);
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(uniRefEntry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        return unirefLightConverter.fromXml(xmlEntry);
    }
}
