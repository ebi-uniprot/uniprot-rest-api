package org.uniprot.api.idmapping.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jodah.failsafe.RetryPolicy;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequestImpl;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
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

@ExtendWith(MockitoExtension.class)
class UniParcIdMappingDownloadResultWriterTest {

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
                        rdfStream);
        Iterator<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("UPI000000000B").build())
                        .iterator();
        String fields = "upi";
        BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry> result =
                writer.getBatchStoreEntryPairIterable(mappedIds, fields);
        assertNotNull(result);
    }

    @Test
    void canWriteResultFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
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

        UniParcIdMappingDownloadResultWriter writer =
                new UniParcIdMappingDownloadResultWriter(
                        contentAdaptor,
                        converterContextFactory,
                        storeStreamConfig,
                        downloadProperties,
                        null);
        List<IdMappingStringPair> mappedIds =
                List.of(IdMappingStringPair.builder().from("P12345").to("UPI0000283A10").build());

        String jobId = "UNIPARC_WRITER_JOB_ID";
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFields("upi");
        request.setFormat("json");
        request.setJobId(jobId);

        IdMappingResult idMappingResult = IdMappingResult.builder().mappedIds(mappedIds).build();

        assertDoesNotThrow(
                () ->
                        writer.writeResult(
                                request, idMappingResult, jobId, MediaType.APPLICATION_JSON));

        Path resultFilePath =
                Path.of("target" + File.separator + jobId + FileType.GZIP.getExtension());
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
                        rdfStream);
        Type result = writer.getType();
        assertNotNull(result);
        assertEquals(
                "org.uniprot.api.rest.output.context.MessageConverterContext<org.uniprot.api.idmapping.model.UniParcEntryPair>",
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