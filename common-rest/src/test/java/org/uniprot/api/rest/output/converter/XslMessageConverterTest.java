package org.uniprot.api.rest.output.converter;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * @author lgonzales
 * @since 2020-04-03
 */
class XslMessageConverterTest {

    private static XslMessageConverter<UniProtKBEntry> xslMessageConverter;

    @BeforeAll
    static void init() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        UniProtKBEntryValueMapper mapper = new UniProtKBEntryValueMapper();
        xslMessageConverter =
                new XslMessageConverter<UniProtKBEntry>(UniProtKBEntry.class, returnFieldConfig, mapper);
    }
    @Test
    void canWriteHeader() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().fields("accession,organism,gene_orf").build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        xslMessageConverter.before(messageContext, outputStream);
        xslMessageConverter.after(messageContext, outputStream);

        InputStream excelInput = new ByteArrayInputStream(outputStream.toByteArray());
        Workbook workbook = new XSSFWorkbook(excelInput);
        assertNotNull(workbook);
        assertEquals(1, workbook.getNumberOfSheets());

        Sheet sheet = workbook.getSheetAt(0);
        assertNotNull(sheet);

        Row row = sheet.getRow(0);
        assertNotNull(row);

        List<String> cellValues = StreamSupport.stream(row.spliterator(), false)
                .map(Cell::getStringCellValue)
                .collect(toList());
        assertNotNull(cellValues);
        assertEquals(3, cellValues.size());
        assertTrue(cellValues.contains("Entry"));
        assertTrue(cellValues.contains("Organism"));
        assertTrue(cellValues.contains("Gene Names (ORF)"));
    }

    @Test
    void canWriteBody() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder().fields("accession,gene_primary").build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        UniProtKBEntry entity = UniProtKBEntryIT.getCompleteColumnsUniProtEntry();
        xslMessageConverter.before(messageContext,outputStream);
        xslMessageConverter.writeEntity(entity, outputStream);
        xslMessageConverter.after(messageContext, outputStream);

        InputStream excelInput = new ByteArrayInputStream(outputStream.toByteArray());
        Workbook workbook = new XSSFWorkbook(excelInput);
        assertNotNull(workbook);
        assertEquals(1, workbook.getNumberOfSheets());

        Sheet sheet = workbook.getSheetAt(0);
        assertNotNull(sheet);

        Row row = sheet.getRow(1);
        assertNotNull(row);

        List<String> cellValues = StreamSupport.stream(row.spliterator(), false)
                .map(Cell::getStringCellValue)
                .collect(toList());
        assertNotNull(cellValues);
        assertEquals(2, cellValues.size());
        assertTrue(cellValues.contains("P00001"));
        assertTrue(cellValues.contains("some Gene"));
    }
}
