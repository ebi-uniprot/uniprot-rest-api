package uk.ac.ebi.uniprot.api.disease.response.converter;

import uk.ac.ebi.uniprot.api.configure.util.SupportingDataUtils;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.parser.tsv.disease.DiseaseEntryMap;
import uk.ac.ebi.uniprot.search.field.DiseaseField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiseaseTsvMessageConverter extends AbstractTsvMessagerConverter<Disease> {
    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    public DiseaseTsvMessageConverter() {
        super(Disease.class);
    }

    @Override
    protected List<String> entry2TsvStrings(Disease entity) {
        Map<String, String> mappedField = new DiseaseEntryMap(entity).attributeValues();
        return getData(mappedField);
    }

    @Override
    protected List<String> getHeader() {
        List<String> fields = this.tlFields.get();
        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());
    }

    private String getFieldDisplayName(String fieldName) {
        return DiseaseField.ResultFields.valueOf(fieldName).getLabel();
    }

    @Override
    protected void initBefore(MessageConverterContext<Disease> context) {
        if(Utils.nullOrEmpty(context.getFields())){
            this.tlFields.set(Arrays.asList(DiseaseField.ResultFields.getDefaultFields().split(SupportingDataUtils.COMMA)));
        } else {
            this.tlFields.set(Arrays.asList(context.getFields().split(SupportingDataUtils.COMMA)));
        }
    }

    public List<String> getData(Map<String, String> mappedField) {
        List<String> fields = this.tlFields.get();

        return fields.stream()
                .map(field -> mappedField.getOrDefault(field,""))
                .collect(Collectors.toList());
    }
}
