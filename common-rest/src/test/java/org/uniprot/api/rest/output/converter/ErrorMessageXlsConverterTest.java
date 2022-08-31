package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/** @author lgonzales */
class ErrorMessageXlsConverterTest {

    @Test
    void canWriteErrorMessage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = getHttpOutputMessage(outputStream);
        ErrorInfo errorInfo = new ErrorInfo("url", Collections.singletonList("message"));
        ErrorMessageXlsConverter converter = new ErrorMessageXlsConverter();
        converter.writeInternal(errorInfo, null, httpOutputMessage);

        InputStream excelInput = new ByteArrayInputStream(outputStream.toByteArray());
        Workbook workbook = new XSSFWorkbook(excelInput);
        assertNotNull(workbook);
        assertEquals(1, workbook.getNumberOfSheets());

        Sheet sheet = workbook.getSheetAt(0);
        assertNotNull(sheet);
        Row row = sheet.getRow(0);
        assertNotNull(row);
        assertEquals("Error messages", row.getCell(0).getStringCellValue());

        row = sheet.getRow(1);
        assertNotNull(row);
        assertEquals("message", row.getCell(0).getStringCellValue());
    }

    @Test
    void canWriteMultipleErrorMessages() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = getHttpOutputMessage(outputStream);
        List<String> messages = Arrays.asList("errorMessage1", "errorMessage2");
        ErrorInfo errorInfo = new ErrorInfo("url", messages);
        ErrorMessageXlsConverter converter = new ErrorMessageXlsConverter();
        converter.writeInternal(errorInfo, null, httpOutputMessage);

        InputStream excelInput = new ByteArrayInputStream(outputStream.toByteArray());
        Workbook workbook = new XSSFWorkbook(excelInput);
        assertNotNull(workbook);
        assertEquals(1, workbook.getNumberOfSheets());

        Sheet sheet = workbook.getSheetAt(0);
        assertNotNull(sheet);
        Row row = sheet.getRow(0);
        assertNotNull(row);
        assertEquals("Error messages", row.getCell(0).getStringCellValue());

        row = sheet.getRow(1);
        assertNotNull(row);
        assertEquals("errorMessage1", row.getCell(0).getStringCellValue());

        row = sheet.getRow(2);
        assertNotNull(row);
        assertEquals("errorMessage2", row.getCell(0).getStringCellValue());
    }

    @Test
    void canWriteIfErrorInfoEntityAndXls() {
        assertTrue(
                new ErrorMessageXlsConverter()
                        .canWrite(null, ErrorInfo.class, UniProtMediaType.XLS_MEDIA_TYPE));
    }

    @Test
    void cannotWriteIfErrorInfoEntityButInvalidFormat() {
        assertFalse(
                new ErrorMessageXlsConverter()
                        .canWrite(null, ErrorInfo.class, MediaType.APPLICATION_JSON));
    }

    @Test
    void cannotWriteIfNotErrorInfoEntityAndFasta() {
        assertFalse(
                new ErrorMessageXlsConverter()
                        .canWrite(null, String.class, UniProtMediaType.XLS_MEDIA_TYPE));
    }

    @Test
    void readInternalThrowsException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new ErrorMessageXlsConverter().readInternal(ErrorInfo.class, null));
    }

    @Test
    void readThrowsException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new ErrorMessageXlsConverter().read(null, ErrorInfo.class, null));
    }

    private HttpOutputMessage getHttpOutputMessage(ByteArrayOutputStream outputStream) {
        return new HttpOutputMessage() {
            @Override
            public OutputStream getBody() {
                return outputStream;
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }
        };
    }
}
