package org.uniprot.api.rest.output.converter;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * @author jluo
 * @date: 1 May 2019
 */
public class XlsMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {
    private static final Logger LOGGER = getLogger(XlsMessageConverter.class);
    private ThreadLocal<SXSSFWorkbook> tlWorkBook = new ThreadLocal<>();
    private ThreadLocal<SXSSFSheet> tlSheet = new ThreadLocal<>();
    private ThreadLocal<AtomicInteger> tlCounter = new ThreadLocal<>();
    private ReturnFieldConfig fieldConfig;
    private EntityValueMapper<T> entityMapper;
    private ThreadLocal<List<ReturnField>> tlFields = new ThreadLocal<>();

    public XlsMessageConverter(
            Class<T> messageConverterEntryClass,
            ReturnFieldConfig fieldConfig,
            EntityValueMapper<T> entityMapper) {
        super(UniProtMediaType.XLS_MEDIA_TYPE, messageConverterEntryClass);
        this.fieldConfig = fieldConfig;
        this.entityMapper = entityMapper;
    }

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream) {
        tlFields.set(OutputFieldsParser.parse(context.getFields(), fieldConfig));
        SXSSFWorkbook workbook = new SXSSFWorkbook(500);
        tlWorkBook.set(workbook);
        tlSheet.set(workbook.createSheet());
        tlCounter.set(new AtomicInteger(0));

        Row header = tlSheet.get().createRow(0);
        updateRow(header, getHeader());
    }

    @Override
    protected void after(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        tlWorkBook.get().write(outputStream);
        tlWorkBook.get().dispose();
    }

    @Override
    protected void cleanUp() {
        try {
            tlWorkBook.get().close();
        } catch (IOException e) {
            LOGGER.error("Problem closing workbook", e);
        }
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        Map<String, String> resultMap = getMappedEntity(entity);
        List<String> result = OutputFieldsParser.getData(resultMap, tlFields.get());

        Row row = tlSheet.get().createRow(tlCounter.get().incrementAndGet());
        updateRow(row, result);
    }

    private Map<String, String> getMappedEntity(T entity) {
        List<String> fieldNames =
                tlFields.get().stream().map(ReturnField::getName).collect(Collectors.toList());
        return entityMapper.mapEntity(entity, fieldNames);
    }

    protected List<String> getHeader() {
        List<ReturnField> fields = tlFields.get();
        return fields.stream().map(ReturnField::getLabel).collect(Collectors.toList());
    }

    private void updateRow(Row row, List<String> result) {
        for (int cellnum = 0; cellnum < result.size(); cellnum++) {
            Cell cell = row.createCell(cellnum);
            cell.setCellValue(result.get(cellnum));
        }
    }
}
