package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * @author jluo
 * @date: 1 May 2019
 */
public class TsvMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private final ReturnFieldConfig fieldConfig;
    private static final ThreadLocal<List<ReturnField>> TL_FIELDS = new ThreadLocal<>();
    private final EntityValueMapper<T> entityMapper;

    public TsvMessageConverter(
            Class<T> messageConverterEntryClass,
            ReturnFieldConfig fieldConfig,
            EntityValueMapper<T> entityMapper) {
        super(UniProtMediaType.TSV_MEDIA_TYPE, messageConverterEntryClass);
        this.fieldConfig = fieldConfig;
        this.entityMapper = entityMapper;
    }

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        TL_FIELDS.set(OutputFieldsParser.parse(context.getFields(), fieldConfig));
        outputStream.write(getHeader().getBytes());
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        List<String> fieldNames =
                TL_FIELDS.get().stream().map(ReturnField::getName).collect(Collectors.toList());
        Map<String, String> resultMap = entityMapper.mapEntity(entity, fieldNames);
        List<String> result = OutputFieldsParser.getData(resultMap, TL_FIELDS.get());
        outputStream.write(result.stream().collect(Collectors.joining("\t", "", "\n")).getBytes());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        TL_FIELDS.remove();
    }

    private String getHeader() {
        List<ReturnField> fields = TL_FIELDS.get();
        return fields.stream()
                .map(ReturnField::getLabel)
                .collect(Collectors.joining("\t", "", "\n"));
    }
}
