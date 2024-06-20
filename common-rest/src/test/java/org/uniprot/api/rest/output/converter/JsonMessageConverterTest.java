package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.api.rest.output.FakePair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 * @since 2020-04-02
 */
@Slf4j
class JsonMessageConverterTest {

    private static JsonMessageConverter<UniProtKBEntry> jsonMessageConverter;

    @BeforeAll
    static void init() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        ObjectMapper objectMapper = UniprotKBJsonConfig.getInstance().getSimpleObjectMapper();
        jsonMessageConverter =
                new JsonMessageConverter<>(objectMapper, UniProtKBEntry.class, returnFieldConfig);
    }

    @Test
    void beforeEntityOnlyReturnEmptyOutput() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().entityOnly(true).build();
        log.debug("------- BEGIN: beforeEntityOnlyReturnEmptyOutput");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void beforeCanPrintFacet() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .facets(Collections.singleton(getFacet()))
                        .build();
        log.debug("------- BEGIN: beforeCanPrintFacet");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.facets")));
        assertEquals(resultJson.read(JsonPath.compile("$.facets.size()")), Integer.valueOf(1));
        assertEquals("My Facet", resultJson.read(JsonPath.compile("$.facets[0].label")));

        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), Integer.valueOf(0));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.matchedFields")));
    }

    @Test
    void beforeCanPrintMatchedFields() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .matchedFields(Collections.singleton(getMatchedField()))
                        .build();
        log.debug("------- BEGIN: beforeCanPrintMatchedFields");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.matchedFields")));
        assertEquals(
                resultJson.read(JsonPath.compile("$.matchedFields.size()")), Integer.valueOf(1));
        assertEquals("fieldName", resultJson.read(JsonPath.compile("$.matchedFields[0].name")));

        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), Integer.valueOf(0));

        assertThrows(
                PathNotFoundException.class, () -> resultJson.read(JsonPath.compile("$.facets")));
    }

    @Test
    void writeCanWriteEntity() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().entityOnly(true).build();
        log.debug("------- BEGIN: writeCanWriteEntity");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P00001", resultJson.read(JsonPath.compile("$.primaryAccession")));
    }

    @Test
    void writeCanWriteEntityInvalidFieldPath() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("accession,gene_synonym,cc_function")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntity");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        UniProtKBEntry simpleEntry =
                new UniProtKBEntryBuilder("P12345", "ID", UniProtKBEntryType.TREMBL).build();
        jsonMessageConverter.writeEntity(simpleEntry, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P12345", resultJson.read(JsonPath.compile("$.primaryAccession")));
    }

    @Test
    void writeCanWriteTenEntity() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().build();
        log.debug("------- BEGIN: 10 writeCanTenWriteEntity");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), Integer.valueOf(10));
    }

    @Test
    void writeCanWriteEntityWithPathOnlyReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("accession,organism_name,gene_primary,gene_synonym")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntityWithPathOnlyReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);

        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P00001", resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(
                "scientific name", resultJson.read(JsonPath.compile("$.organism.scientificName")));
        assertEquals("some Gene", resultJson.read(JsonPath.compile("$.genes[0].geneName.value")));
        assertEquals("some Syn", resultJson.read(JsonPath.compile("$.genes[0].synonyms[0].value")));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntityWithFilteredPathReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("accession,cc_function")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntityWithFilteredPathReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("P00001", resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals("FUNCTION", resultJson.read(JsonPath.compile("$.comments[0].commentType")));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntityWithFilteredPathWithOrLogicReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("organism_name,cc_rna_editing,cc_polymorphism")
                        .build();
        log.debug("------- BEGIN: writeCanWriteEntityWithFilteredPathWithOrLogicReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(
                "P00001",
                resultJson.read(JsonPath.compile("$.primaryAccession"))); // required field
        assertEquals(
                "scientific name", resultJson.read(JsonPath.compile("$.organism.scientificName")));
        assertEquals("RNA EDITING", resultJson.read(JsonPath.compile("$.comments[0].commentType")));
        assertEquals(
                "POLYMORPHISM", resultJson.read(JsonPath.compile("$.comments[1].commentType")));

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntitiesWithFilteredPathAndCanPrintEntitySeparator() throws IOException {
        List<UniProtKBEntry> entities = new ArrayList<>();
        entities.add(getEntity());
        entities.add(getEntity());
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().fields("accession").build();
        log.debug("------- BEGIN: writeCanWriteEntitiesWithFilteredPathAndCanPrintEntitySeparator");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                entities.stream(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals(
                "{\"results\":[{\"entryType\":\"UniProtKB reviewed (Swiss-Prot)\",\"primaryAccession\":\"P00001\",\"extraAttributes\":{\"uniParcId\":\"UP1234567890\"}},\n"
                        + "{\"entryType\":\"UniProtKB reviewed (Swiss-Prot)\",\"primaryAccession\":\"P00001\",\"extraAttributes\":{\"uniParcId\":\"UP1234567890\"}}]}",
                result);
    }

    @Test
    void writeCanWriteOkayAndExtraOptionsEntities() throws IOException {
        List<UniProtKBEntry> entities = new ArrayList<>();
        entities.add(getEntity());
        ExtraOptions extraOptions =
                ExtraOptions.builder()
                        .failedIds(List.of("id1"))
                        .suggestedId(FakePair.builder().from("fromid2").to("toid2").build())
                        .obsoleteCount(10)
                        .build();
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession")
                        .extraOptions(extraOptions)
                        .build();
        log.debug("------- BEGIN: writeCanWriteOkayAndFailedEntities");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                entities.stream(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals(
                "{\"results\":[{\"entryType\":\"UniProtKB reviewed (Swiss-Prot)\",\"primaryAccession\":\"P00001\",\"extraAttributes\":{\"uniParcId\":\"UP1234567890\"}}],\"failedIds\":[\"id1\"],\"suggestedIds\":[{\"from\":\"fromid2\",\"to\":\"toid2\"}],\"obsoleteCount\":10}",
                result);
    }

    @Test
    void writeCanWriteOnlyFailedEntities() throws IOException {
        ExtraOptions extraOptions = ExtraOptions.builder().failedIds(List.of("id1", "id2")).build();
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .extraOptions(extraOptions)
                        .build();
        log.debug("------- BEGIN: writeCanWriteOnlyFailedEntities");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                Stream.empty(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals("{\"results\":[],\"failedIds\":[\"id1\",\"id2\"]}", result);
    }

    @Test
    void writeCanWriteOnlySuggestedIdsEntities() throws IOException {
        ExtraOptions extraOptions =
                ExtraOptions.builder()
                        .suggestedId(FakePair.builder().from("fromid3").to("toid3").build())
                        .suggestedId(FakePair.builder().from("fromid4").to("toid4").build())
                        .build();
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .extraOptions(extraOptions)
                        .build();
        log.debug("------- BEGIN: writeCanWriteOnlyFailedEntities");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                Stream.empty(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals(
                "{\"results\":[],\"suggestedIds\":[{\"from\":\"fromid3\",\"to\":\"toid3\"},{\"from\":\"fromid4\",\"to\":\"toid4\"}]}",
                result);
    }

    @Test
    void writeCanWriteOnlySuggestions() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .suggestions(
                                List.of(
                                        Suggestion.builder().query("one").hits(1).build(),
                                        Suggestion.builder().query("one").hits(1).build()))
                        .build();
        log.debug("------- BEGIN: writeCanWriteOnlySuggestions");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                Stream.empty(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals(
                "{\"results\":[],\"suggestions\":[{\"query\":\"one\",\"hits\":1},{\"query\":\"one\",\"hits\":1}]}",
                result);
    }

    @Test
    void writeCanWriteTenEntitiesWithFilteredPathWithOrLogicReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("organism_name,cc_rna_editing,cc_polymorphism")
                        .build();
        log.debug(
                "------- BEGIN: 10 writeCanWriteTenEntitiesWithFilteredPathWithOrLogicReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), Integer.valueOf(10));
    }

    @Test
    void writeStopStreamErrorMessage() throws IOException {
        List<UniProtKBEntry> entities = new ArrayList<>();
        entities.add(getEntity());
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entities(entities.stream())
                        .build();

        log.debug("------- BEGIN: writeStopStreamErrorMessage");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);

        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        ObjectMapper objectMapper = UniprotKBJsonConfig.getInstance().getSimpleObjectMapper();
        ErrorJsonMessageConverter<UniProtKBEntry> errorJsonMessageConverter =
                new ErrorJsonMessageConverter<>(
                        objectMapper, UniProtKBEntry.class, returnFieldConfig);

        assertThrows(
                StopStreamException.class,
                () ->
                        errorJsonMessageConverter.writeContents(
                                messageContext, outputStream, Instant.now(), new AtomicInteger(0)));
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals(
                "{\"results\":[],\"error\":\"Error encountered when streaming data.\"}", result);
    }

    @Test
    void writeCanWriteOnlyObsoleteCount() throws IOException {
        ExtraOptions extraOptions = ExtraOptions.builder().obsoleteCount(15).build();
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .extraOptions(extraOptions)
                        .build();
        log.debug("------- BEGIN: writeCanWriteOnlyFailedEntities");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        jsonMessageConverter.writeEntities(
                Stream.empty(), outputStream, Instant.now(), new AtomicInteger(0));
        writeAfter(messageContext, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);
        log.debug(result);
        assertEquals("{\"results\":[],\"obsoleteCount\":15}", result);
    }

    private void writeBefore(
            MessageConverterContext<UniProtKBEntry> messageContext,
            ByteArrayOutputStream outputStream)
            throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.before(messageContext, outputStream);
        long end = System.currentTimeMillis();
        log.debug("DEBUG: Before " + (end - start) + " MilliSeconds");
    }

    private void writeEntity(OutputStream outputStream) throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.writeEntity(getEntity(), outputStream);
        long end = System.currentTimeMillis();
        log.debug("DEBUG: Write " + (end - start) + " MilliSeconds");
    }

    private void writeAfter(
            MessageConverterContext<UniProtKBEntry> messageContext,
            ByteArrayOutputStream outputStream)
            throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.after(messageContext, outputStream);
        jsonMessageConverter.cleanUp();
        long end = System.currentTimeMillis();
        log.debug("DEBUG: After " + (end - start) + " MilliSeconds");
    }

    private UniProtKBEntry getEntity() {
        return UniProtKBEntryIT.getCompleteColumnsUniProtEntry();
    }

    private Facet getFacet() {
        FacetItem item =
                FacetItem.builder().label("Item label").count(10L).value("item_value").build();

        return Facet.builder()
                .name("my_facet")
                .label("My Facet")
                .allowMultipleSelection(true)
                .values(Collections.singletonList(item))
                .build();
    }

    private TermInfo getMatchedField() {
        return TermInfo.builder().hits(10).name("fieldName").build();
    }

    private static class ErrorJsonMessageConverter<T> extends JsonMessageConverter<T> {

        ErrorJsonMessageConverter(
                ObjectMapper objectMapper,
                Class<T> messageConverterEntryClass,
                ReturnFieldConfig returnFieldConfig) {
            super(objectMapper, messageConverterEntryClass, returnFieldConfig);
        }

        ErrorJsonMessageConverter(
                ObjectMapper objectMapper,
                Class<T> messageConverterEntryClass,
                ReturnFieldConfig returnFieldConfig,
                Gatekeeper downloadGatekeeper) {
            super(objectMapper, messageConverterEntryClass, returnFieldConfig, downloadGatekeeper);
        }

        @Override
        protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
            throw new StopStreamException("Fake error to reproduce", new NullPointerException());
        }
    }
}
