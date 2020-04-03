package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * @author lgonzales
 * @since 2020-04-02
 */
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
        System.out.println("------- BEGIN: beforeEntityOnlyReturnEmptyOutput");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void beforeCanPrintFacet() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .facets(Collections.singleton(getFacet()))
                        .build();
        System.out.println("------- BEGIN: beforeCanPrintFacet");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.facets")));
        assertEquals(resultJson.read(JsonPath.compile("$.facets.size()")), new Integer(1));
        assertEquals(resultJson.read(JsonPath.compile("$.facets[0].label")), "My Facet");

        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(0));

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
        System.out.println("------- BEGIN: beforeCanPrintMatchedFields");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.matchedFields")));
        assertEquals(resultJson.read(JsonPath.compile("$.matchedFields.size()")), new Integer(1));
        assertEquals(resultJson.read(JsonPath.compile("$.matchedFields[0].name")), "fieldName");

        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(0));

        assertThrows(
                PathNotFoundException.class, () -> resultJson.read(JsonPath.compile("$.facets")));
    }

    @Test
    void writeCanWriteEntity() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().entityOnly(true).build();
        System.out.println("------- BEGIN: writeCanWriteEntity");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        System.out.println(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(resultJson.read(JsonPath.compile("$.primaryAccession")), "P00001");
    }

    @Test
    void writeCanWriteTenEntity() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().build();
        System.out.println("------- BEGIN: 10 writeCanTenWriteEntity");
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

        String result = outputStream.toString("UTF-8");
        System.out.println(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(10));
    }

    @Test
    void writeCanWriteEntityWithPathOnlyReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("accession,organism,gene_primary,gene_synonym")
                        .build();
        System.out.println("------- BEGIN: writeCanWriteEntityWithPathOnlyReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        System.out.println(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);

        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(resultJson.read(JsonPath.compile("$.primaryAccession")), "P00001");
        assertEquals(
                resultJson.read(JsonPath.compile("$.organism.scientificName")), "scientific name");
        assertEquals(resultJson.read(JsonPath.compile("$.genes[0].geneName.value")), "some Gene");
        assertEquals(resultJson.read(JsonPath.compile("$.genes[0].synonyms[0].value")), "some Syn");

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
        System.out.println("------- BEGIN: writeCanWriteEntityWithFilteredPathReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        System.out.println(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(resultJson.read(JsonPath.compile("$.primaryAccession")), "P00001");
        assertEquals(resultJson.read(JsonPath.compile("$.comments[0].commentType")), "FUNCTION");

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteEntityWithFilteredPathWithOrLogicReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .entityOnly(true)
                        .fields("organism,cc_rna_editing,cc_polymorphism")
                        .build();
        System.out.println(
                "------- BEGIN: writeCanWriteEntityWithFilteredPathWithOrLogicReturnField");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writeBefore(messageContext, outputStream);
        writeEntity(outputStream);
        writeAfter(messageContext, outputStream);

        String result = outputStream.toString("UTF-8");
        System.out.println(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.primaryAccession")));
        assertEquals(
                resultJson.read(JsonPath.compile("$.primaryAccession")),
                "P00001"); // required field
        assertEquals(
                resultJson.read(JsonPath.compile("$.organism.scientificName")), "scientific name");
        assertEquals(resultJson.read(JsonPath.compile("$.comments[0].commentType")), "RNA EDITING");
        assertEquals(
                resultJson.read(JsonPath.compile("$.comments[1].commentType")), "POLYMORPHISM");

        assertThrows(
                PathNotFoundException.class,
                () -> resultJson.read(JsonPath.compile("$.secondaryAccessions")));
    }

    @Test
    void writeCanWriteTenEntitiesWithFilteredPathWithOrLogicReturnField() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("organism,cc_rna_editing,cc_polymorphism")
                        .build();
        System.out.println(
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

        String result = outputStream.toString("UTF-8");
        System.out.println(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        DocumentContext resultJson = JsonPath.parse(result);
        assertNotNull(resultJson.read(JsonPath.compile("$.results")));
        assertEquals(resultJson.read(JsonPath.compile("$.results.size()")), new Integer(10));
    }

    private void writeBefore(
            MessageConverterContext<UniProtKBEntry> messageContext,
            ByteArrayOutputStream outputStream)
            throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.before(messageContext, outputStream);
        long end = System.currentTimeMillis();
        System.out.println("DEBUG: Before " + (end - start) + " MilliSeconds");
    }

    private void writeEntity(OutputStream outputStream) throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.writeEntity(getEntity(), outputStream);
        long end = System.currentTimeMillis();
        System.out.println("DEBUG: Write " + (end - start) + " MilliSeconds");
    }

    private void writeAfter(
            MessageConverterContext<UniProtKBEntry> messageContext,
            ByteArrayOutputStream outputStream)
            throws IOException {
        long start = System.currentTimeMillis();
        jsonMessageConverter.after(messageContext, outputStream);
        long end = System.currentTimeMillis();
        System.out.println("DEBUG: After " + (end - start) + " MilliSeconds");
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
}
