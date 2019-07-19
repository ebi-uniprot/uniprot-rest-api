package uk.ac.ebi.uniprot.api.rest.output.converter;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.search.field.ReturnField;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sahmad
 *
 * Returns a map of root level fields with their values for the json writer
 */
@Slf4j
public class JsonResponseFieldProjector {

    // Get a map containing the object's projected fields
    public Map<String, Object> project(Object object, List<String> returnFields, List<ReturnField> allFields) {

        List<String> camelCaseFieldNames;
        if(!Utils.notEmpty(returnFields)){ // if the return fields is empty return all fields from allFields
            camelCaseFieldNames = allFields.stream().map(f -> f.getJavaFieldName()).collect(Collectors.toList());
        } else {
            camelCaseFieldNames = allFields.stream().filter(f -> returnFields.contains(f.toString())).map(f -> f.getJavaFieldName()).collect(Collectors.toList());

        }

        // create the Map instance to be returned with root fields populated
        Map<String, Object> projectedMap = buildMapWithFields(object, camelCaseFieldNames, allFields);

        return projectedMap;

    }

    // Get a map with provided field names and values
    private Map<String, Object> buildMapWithFields(Object source, List<String> fieldNames, List<ReturnField> allFields) {

        Map<String, Object> target = new HashMap<>();

        for (String fieldName : fieldNames) {
            String javaFieldName = getJavaFieldName(fieldName, allFields);
            Object fieldValue = getFieldValue(source, javaFieldName);
            if (fieldValue != null) {
                target.put(fieldName, fieldValue);
            }
        }

        return target;

    }

    private String getJavaFieldName(String fieldName, List<ReturnField> allFields) {
     return allFields.stream()
                .filter(f -> fieldName.equals(f.toString()))
                .map(f -> f.getJavaFieldName()).findFirst().orElse(fieldName);

    }

    // get the value of a given field
    private Object getFieldValue(Object source, String fieldName) {
        try {
            Field sourceField = source.getClass().getDeclaredField(fieldName);
            sourceField.setAccessible(true);
            return sourceField.get(source);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("{} not found", fieldName);// incorrect fieldName provided
            return null;
        }
    }

}