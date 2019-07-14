package uk.ac.ebi.uniprot.api.disease.response.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.field.DiseaseField;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class DiseaseJsonMessageConverter extends AbstractJsonMessageConverter<Disease> {

    public DiseaseJsonMessageConverter() {
        super(DiseaseJsonConfig.getInstance().getSimpleObjectMapper(), Disease.class, Arrays.asList(DiseaseField.ResultFields.values()));
    }

    @Override
    protected Disease filterEntryContent(Disease entity) {
       return null;// Do Nothing TODO remove filterEntryContent from super class once all json converter starts using method projectEntryFields
    }

    @Override
    protected void writeEntity(Disease entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = getThreadLocalJsonGenerator().get();
        generator.writeObject(projectEntryFields(entity));
    }
}