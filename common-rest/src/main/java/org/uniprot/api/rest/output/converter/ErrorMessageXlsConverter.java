package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/**
 * This class is responsible to write XLS error message body for http status BAD REQUESTS (400)
 *
 * @author lgonzales
 */
public class ErrorMessageXlsConverter extends AbstractGenericHttpMessageConverter<ErrorInfo> {

    public ErrorMessageXlsConverter() {
        super(UniProtMediaType.XLS_MEDIA_TYPE);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ErrorInfo.class);
    }

    @Override
    protected void writeInternal(
            ErrorInfo errorInfo, Type type, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (errorInfo.getMessages() != null) {
            OutputStream outputStream = httpOutputMessage.getBody();
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(errorInfo.getMessages().size() + 1)) {

                SXSSFSheet sheet = (workbook.createSheet());
                Row headerRow = sheet.createRow(0);
                Cell headerCell = headerRow.createCell(0);
                headerCell.setCellValue("Error messages");
                int rowNum = 1;
                for (String message : errorInfo.getMessages()) {
                    Row messageRow = sheet.createRow(rowNum);
                    Cell messageCell = messageRow.createCell(0);
                    messageCell.setCellValue(message);
                    rowNum++;
                }
                workbook.write(outputStream);
                workbook.dispose();
            }
        }
    }

    @Override
    protected ErrorInfo readInternal(
            Class<? extends ErrorInfo> aClass, HttpInputMessage httpInputMessage)
            throws HttpMessageNotReadableException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public ErrorInfo read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("");
    }
}
