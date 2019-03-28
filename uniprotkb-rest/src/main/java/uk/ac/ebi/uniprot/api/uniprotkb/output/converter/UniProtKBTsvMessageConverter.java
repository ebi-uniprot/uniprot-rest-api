package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.service.filters.EntryFilters;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.parser.tsv.uniprot.EntryMap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class UniProtKBTsvMessageConverter extends AbstractEntityHttpMessageConverter<UniProtEntry> {
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public UniProtKBTsvMessageConverter() {
        super(UniProtMediaType.TSV_MEDIA_TYPE);
    }

    @Override
    protected void before(MessageConverterContext context, OutputStream outputStream) throws IOException {
        tlFilters.set(FieldsParser.parseForFilters(context.getFields()));
        tlFields.set(FieldsParser.parse(context.getFields()));
        outputStream.write(convertHeader(tlFields.get()).stream().collect(Collectors.joining("\t", "", "\n")).getBytes());
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        List<String> result = entry2TsvStrings(entity, tlFilters.get(), tlFields.get());
        outputStream.write(result.stream().collect(Collectors.joining("\t","", "\n")).getBytes());
    }

    private List<String> convertHeader(List<String> fields) {
        return fields.stream().map(this::getFieldDisplayName)
                .collect(Collectors.toList());
    }

    private String getFieldDisplayName(String field) {
        Optional<Field> opField = UniProtResultFields.INSTANCE.getField(field);
        if (opField.isPresent())
            return opField.get().getLabel();
        else
            return field;
    }

    private List<String> entry2TsvStrings(UniProtEntry entry, Map<String, List<String>> filterParams, List<String> fields) {
        if ((filterParams != null) && !filterParams.isEmpty())
            EntryFilters.filterEntry(entry, filterParams);
        EntryMap dlEntry = new EntryMap(entry, fields);
        return dlEntry.getData();
    }
}
