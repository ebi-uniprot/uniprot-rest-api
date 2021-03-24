package org.uniprot.api.uniparc.output.converter;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.uniparc.model.UniParcEntryWrapper;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.model.ReturnField;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UniParcTsvMessageConverter extends AbstractUUWHttpMessageConverter<UniParcEntryWrapper,
        UniParcCrossReference> {
    private final ReturnFieldConfig fieldConfig;
    private static final ThreadLocal<List<ReturnField>> TL_FIELDS = new ThreadLocal<>();
    private final EntityValueMapper<UniParcCrossReference> entityMapper;

    public UniParcTsvMessageConverter(
            Class<UniParcEntryWrapper> messageConverterEntryClass,
            ReturnFieldConfig fieldConfig,
            EntityValueMapper<UniParcCrossReference> entityMapper) {
        super(UniProtMediaType.TSV_MEDIA_TYPE, messageConverterEntryClass);
        this.fieldConfig = fieldConfig;
        this.entityMapper = entityMapper;
    }

    @Override
    protected Stream<UniParcCrossReference> entitiesToWrite(MessageConverterContext<UniParcEntryWrapper> context) {
        UniParcEntryWrapper entryWrapper = context.getEntities().collect(Collectors.toList()).get(0);
        return entryWrapper.getEntry().getUniParcCrossReferences().stream();
    }

    @Override
    protected void writeEntity(UniParcCrossReference entity, OutputStream outputStream) throws IOException {
        List<String> fieldNames =
                TL_FIELDS.get().stream().map(ReturnField::getName).collect(Collectors.toList());
        Map<String, String> resultMap = entityMapper.mapEntity(entity, fieldNames);
        List<String> result = OutputFieldsParser.getData(resultMap, TL_FIELDS.get());
        outputStream.write(result.stream().collect(Collectors.joining("\t", "", "\n")).getBytes());
    }


    @Override
    protected void before(MessageConverterContext<UniParcEntryWrapper> context, OutputStream outputStream)
            throws IOException {
        TL_FIELDS.set(OutputFieldsParser.parse(context.getFields(), fieldConfig));
        outputStream.write(getHeader().getBytes());
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
