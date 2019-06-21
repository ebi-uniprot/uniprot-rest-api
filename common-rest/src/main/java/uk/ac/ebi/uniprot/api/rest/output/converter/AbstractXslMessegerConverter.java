package uk.ac.ebi.uniprot.api.rest.output.converter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;


/**
 *
 * @author jluo
 * @date: 1 May 2019
 *
*/

public abstract class AbstractXslMessegerConverter<T> extends AbstractEntityHttpMessageConverter<T> {
	 private static final Logger LOGGER = getLogger(AbstractXslMessegerConverter.class);
    private ThreadLocal<SXSSFWorkbook> tlWorkBook = new ThreadLocal<>();
    private ThreadLocal<SXSSFSheet> tlSheet = new ThreadLocal<>();
    private ThreadLocal<AtomicInteger> tlCounter = new ThreadLocal<>();

    public AbstractXslMessegerConverter(Class<T> messageConverterEntryClass) {
        super(UniProtMediaType.XLS_MEDIA_TYPE, messageConverterEntryClass);
    }
    
	abstract protected List<String> entry2TsvStrings(T entity);
	abstract protected List<String> getHeader();
	abstract protected void initBefore(MessageConverterContext<T> context);
	

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream) {
    	initBefore(context);
        SXSSFWorkbook workbook = new SXSSFWorkbook(500);
        tlWorkBook.set(workbook);
        tlSheet.set(workbook.createSheet());
        tlCounter.set(new AtomicInteger(0));

        Row header = tlSheet.get().createRow(0);
        updateRow(header, getHeader());
    }

    @Override
    protected void after(MessageConverterContext<T> context, OutputStream outputStream) throws IOException {
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
        List<String> result = entry2TsvStrings(entity);

        Row row = tlSheet.get().createRow(tlCounter.get().incrementAndGet());
        updateRow(row, result);
    }

    private void updateRow(Row row, List<String> result) {
        for (int cellnum = 0; cellnum < result.size(); cellnum++) {
            Cell cell = row.createCell(cellnum);
            cell.setCellValue(result.get(cellnum));
        }
    }


}

