package org.uniprot.api.uniprotkb.controller.request;

import java.util.*;
import java.util.stream.Collectors;

import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMetaReader;

public class ReturnFieldMetaReaderImpl implements ModelFieldMetaReader {

    @Override
    public List<Map<String, Object>> read(String metaFile) {
        // e.g. metaFile=uniprotkb-return-fields.json
        List<Map<String, Object>> metaList = new ArrayList<>();
        String[] fileNameTokens = metaFile.split("-");
        if (fileNameTokens != null && fileNameTokens.length > 1) {
            String sourceName = fileNameTokens[0];
            UniProtDataType upDataType = UniProtDataType.valueOf(sourceName.toUpperCase());
            ReturnFieldConfig config = ReturnFieldConfigFactory.getReturnFieldConfig(upDataType);
            metaList =
                    config.getReturnFields().stream()
                            .filter(fieldItem -> fieldItem.isIncludeInSwagger())
                            .sorted(Comparator.comparing(ReturnField::getName))
                            .map(this::convertToMap)
                            .collect(Collectors.toList());
        }

        return metaList;
    }

    private Map<String, Object> convertToMap(ReturnField fieldItem) {
        Map<String, Object> keyValueMap = new LinkedHashMap<>();
        keyValueMap.put("name", fieldItem.getName());
        keyValueMap.put("label", fieldItem.getLabel());
        return keyValueMap;
    }
}
