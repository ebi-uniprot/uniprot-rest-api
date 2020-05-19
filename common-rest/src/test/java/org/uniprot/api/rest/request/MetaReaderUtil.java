package org.uniprot.api.rest.request;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author lgonzales
 * @since 18/05/2020
 */
class MetaReaderUtil {

    static boolean validateFieldMap(
            List<Map<String, Object>> result, String fieldName, String fieldValue) {
        return result.stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(name -> Objects.equals(name.getKey(), fieldName))
                .map(Map.Entry::getValue)
                .anyMatch(value -> Objects.equals(value, fieldValue));
    }
}
