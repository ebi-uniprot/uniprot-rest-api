package uk.ac.ebi.uniprot.api.crossref.output.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.json.parser.crossref.CrossRefJsonConfig;
import uk.ac.ebi.uniprot.search.field.CrossRefField;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class CrossRefJsonMessageConverter extends AbstractJsonMessageConverter<CrossRefEntry> {

    public CrossRefJsonMessageConverter() {
        super(CrossRefJsonConfig.getInstance().getSimpleObjectMapper(), CrossRefEntry.class, Arrays.asList(CrossRefField.ResultFields.values()));
    }

    @Override
    protected CrossRefEntry filterEntryContent(CrossRefEntry entity) {
       return null;// Do Nothing TODO remove filterEntryContent from super class once all json converter starts using method projectEntryFields
    }

    @Override
    protected void writeEntity(CrossRefEntry entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = getThreadLocalJsonGenerator().get();
        generator.writeObject(projectEntryFields(entity));
    }
}