package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.uniprotkb;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.AbstractUUWHttpMessageConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.download.DownloadableEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class UniProtKBXslMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext, UniProtEntry> {
    private static final Logger LOGGER = getLogger(UniProtKBXslMessageConverter.class);
    
    private final Function<UniProtEntry, UPEntry> entryConverter = new EntryConverter();
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();
    private ThreadLocal<SXSSFWorkbook> tlWorkBook = new ThreadLocal<>();
    private ThreadLocal<SXSSFSheet> tlSheet = new ThreadLocal<>();
    private ThreadLocal<AtomicInteger> tlCounter = new ThreadLocal<>();

    public UniProtKBXslMessageConverter() {
        super(UniProtMediaType.XLS_MEDIA_TYPE);
    }

    @Override
    protected void init(MessageConverterContext context) {
        tlFilters.set(FieldsParser.parseForFilters(context.getFields()));
        tlFields.set(FieldsParser.parse(context.getFields()));
        SXSSFWorkbook workbook = new SXSSFWorkbook(500);
        tlWorkBook.set(workbook);
        tlSheet.set(workbook.createSheet());
        tlCounter.set(new AtomicInteger(0));
    }

    @Override
    protected void before(MessageConverterContext context, OutputStream outputStream) {
        Row header = tlSheet.get().createRow(0);
        updateRow(header, convertHeader(tlFields.get()));
    }

    @Override
    protected void after(MessageConverterContext context, OutputStream outputStream) throws IOException {
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
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        List<String> result = entry2TsvStrings(entity, tlFilters.get(), tlFields.get());

        Row row = tlSheet.get().createRow(tlCounter.get().incrementAndGet());
        updateRow(row, result);
    }

    private List<String> convertHeader(List<String> fields) {
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    private void updateRow(Row row, List<String> result) {
        for (int cellnum = 0; cellnum < result.size(); cellnum++) {
            Cell cell = row.createCell(cellnum);
            cell.setCellValue(result.get(cellnum));
        }
    }

    private String getFieldDisplayName(String field) {
        Optional<Field> opField = UniProtResultFields.INSTANCE.getField(field);
        if (opField.isPresent())
            return opField.get().getLabel();
        else
            return field;
    }

    private List<String> entry2TsvStrings(UniProtEntry upEntry, Map<String, List<String>> filterParams, List<String> fields) {
        UPEntry entry = entryConverter.apply(upEntry);
        if ((filterParams != null) && !filterParams.isEmpty())
            EntryFilters.filterEntry(entry, filterParams);
        DownloadableEntry dlEntry = new DownloadableEntry(entry, fields);
        return dlEntry.getData();
    }
}
