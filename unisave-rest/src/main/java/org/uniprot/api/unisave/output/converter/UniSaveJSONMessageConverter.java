package org.uniprot.api.unisave.output.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;

import java.util.Map;

/**
 * @author eddturner
 */
public class UniSaveJSONMessageConverter extends JsonMessageConverter<UniSaveEntry> {

    public UniSaveJSONMessageConverter() {
        super(new ObjectMapper(), UniSaveEntry.class, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> projectEntryFields(UniSaveEntry entity) {
        return objectMapper.convertValue(entity, Map.class);
    }
}
