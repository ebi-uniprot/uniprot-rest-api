package org.uniprot.api.uniprotkb.controller.request;

import java.util.*;
import java.util.stream.Collectors;

import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMetaReader;

public class QueryFieldMetaReaderImpl implements ModelFieldMetaReader {

    @Override
    public List<Map<String, Object>> read(String metaFile) {
        // e.g. metaFile=uniprotkb-search-fields.json
        List<Map<String, Object>> metaList = new ArrayList<>();
        String[] fileNameTokens = metaFile.split("-");
        if (fileNameTokens != null && fileNameTokens.length > 1) {
            String sourceName = fileNameTokens[0];
            UniProtDataType upDataType = UniProtDataType.valueOf(sourceName.toUpperCase());
            SearchFieldConfig config = SearchFieldConfigFactory.getSearchFieldConfig(upDataType);
            metaList =
                    config.getSearchFieldItems().stream()
                            .filter(searchFieldItem -> searchFieldItem.isIncludeInSwagger())
                            .sorted(Comparator.comparing(SearchFieldItem::getFieldName))
                            .map(this::convertToMap)
                            .collect(Collectors.toList());
        }

        return metaList;
    }

    private Map<String, Object> convertToMap(SearchFieldItem searchFieldItem) {
        Map<String, Object> keyValueMap = new LinkedHashMap<>();
        keyValueMap.put("name", searchFieldItem.getFieldName());
        keyValueMap.put("description", searchFieldItem.getDescription());
        keyValueMap.put("dataType", searchFieldItem.getDataType().name().toLowerCase());
        keyValueMap.put("example", searchFieldItem.getExample());
        if (searchFieldItem.getValidRegex() == null) {
            keyValueMap.put("regex", "");
        } else {
            keyValueMap.put("regex", searchFieldItem.getValidRegex());
        }
        return keyValueMap;
    }
}
