package org.uniprot.api.rest.output.converter;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.uniprot.core.uniprotkb.UniProtkbEntry;
import org.uniprot.core.uniprotkb.comment.Comment;
import org.uniprot.core.uniprotkb.feature.Feature;
import org.uniprot.core.uniprotkb.xdb.UniProtkbCrossReference;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.ReturnField;

/**
 * @author sahmad
 *     <p>Returns a map of root level fields with their values for the json writer
 */
@Slf4j
public class JsonResponseFieldProjector {

    // Get a map containing the object's projected fields
    public Map<String, Object> project(
            Object entry, Map<String, List<String>> filterFieldMap, List<ReturnField> allFields) {

        // get map of java field name with its filter value as list
        Map<String, List<String>> javaFieldNameValuesMap =
                getJavaFieldNameFilterValuesMap(filterFieldMap, allFields);

        // create the Map instance to be returned with root fields populated
        Map<String, Object> projectedMap = buildMapWithFields(entry, javaFieldNameValuesMap);

        return projectedMap;
    }

    private Map<String, List<String>> getJavaFieldNameFilterValuesMap(
            Map<String, List<String>> filterFieldMap, List<ReturnField> allFields) {
        Map<String, List<String>>
                javaFieldNameValuesMap; // map to keep java field name with its values if any

        if (Utils.nullOrEmpty(
                filterFieldMap)) { // if the filter field map is empty return all fields from
            // allFields
            javaFieldNameValuesMap =
                    allFields.stream()
                            .filter(f -> f.getJavaFieldName() != null)
                            .collect(
                                    Collectors.toMap(
                                            ReturnField::getJavaFieldName,
                                            f -> Collections.emptyList(),
                                            (oldKey, newKey) -> oldKey,
                                            LinkedHashMap::new));
        } else {
            javaFieldNameValuesMap =
                    allFields.stream()
                            .filter(f -> isResponseField(f, filterFieldMap))
                            .collect(
                                    Collectors.toMap(
                                            ReturnField::getJavaFieldName,
                                            f ->
                                                    filterFieldMap.getOrDefault(
                                                            f.toString(), Collections.emptyList()),
                                            (oldKey, newKey) -> oldKey,
                                            LinkedHashMap::new));
        }

        return javaFieldNameValuesMap;
    }

    // whether this field will be returned in json response or not.
    private boolean isResponseField(ReturnField rf, Map<String, List<String>> filterFieldMap) {

        if ((filterFieldMap.containsKey(rf.toString()) || rf.isMandatoryJsonField())
                && rf.getJavaFieldName() != null) {
            return true;
        }

        return false;
    }

    // Get a map with provided field names and values
    private Map<String, Object> buildMapWithFields(
            Object source, Map<String, List<String>> camelCaseFieldNameValuesMap) {

        Map<String, Object> target = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : camelCaseFieldNameValuesMap.entrySet()) {
            String javaFieldName = entry.getKey();
            List<String> values =
                    entry.getValue(); // values to be returned e.g. comment with type function
            // and domain
            Object fieldValue = getFieldValue(source, javaFieldName, values);
            if (fieldValue != null) {
                target.put(javaFieldName, fieldValue);
            }
        }

        return target;
    }

    // get the value of a given field from source which matches values in neededFieldValues
    private Object getFieldValue(Object source, String fieldName, List<String> neededFieldValues) {
        try {
            Field sourceField = getField(source.getClass(), fieldName);
            if (sourceField != null) {
                sourceField.setAccessible(true);
                Object fieldValue =
                        sourceField.get(
                                source); // extract the value of sourceField from source object
                if (source instanceof UniProtkbEntry) { // check the values for UniProtkbEntry
                    fieldValue = getObjectsWithValues(fieldValue, neededFieldValues);
                }
                return fieldValue;
            }
        } catch (IllegalAccessException e) {
            log.warn("Unable to access field {} through reflection", fieldName);
        }
        return null;
    }

    private Field getField(Class<?> source, String fieldName) {
        Field field = null;
        try {
            field = source.getDeclaredField(fieldName);
        } catch (
                NoSuchFieldException
                        nsf) { // go up the hierarchy to see if the field is defined in parent class
            if (source.getSuperclass() != Object.class) {
                field = getField(source.getSuperclass(), fieldName);
            } else {
                log.warn("{} not found", fieldName); // incorrect fieldName provided
            }
        }

        return field;
    }

    private Object getObjectsWithValues(Object fieldValue, List<String> neededFieldValues) {
        if (Utils.notNullNotEmpty(neededFieldValues)
                && fieldValue != null
                && fieldValue instanceof List<?>
                && Utils.notNullNotEmpty((List<?>) fieldValue)) {
            // comment
            if (((List<?>) fieldValue).get(0)
                    instanceof Comment) { // check one to decide the type of list items
                Predicate<Comment> filter =
                        UniProtkbEntryFilters.createCommentFilter(neededFieldValues);
                List<Comment> comments = (List<Comment>) fieldValue;
                comments.removeIf(comment -> !filter.test(comment));
                return comments;
            } else if (((List<?>) fieldValue).get(0) instanceof Feature) { // feature
                Predicate<Feature> filter =
                        UniProtkbEntryFilters.createFeatureFilter(neededFieldValues);
                List<Feature> features = (List<Feature>) fieldValue;
                features.removeIf(feature -> !filter.test(feature));
                return features;

            } else if (((List<?>) fieldValue).get(0)
                    instanceof UniProtkbCrossReference) { // cross ref
                Predicate<UniProtkbCrossReference> filter =
                        UniProtkbEntryFilters.createDbReferenceFilter(neededFieldValues);
                List<UniProtkbCrossReference> crossReferences =
                        (List<UniProtkbCrossReference>) fieldValue;
                crossReferences.removeIf(xref -> !filter.test(xref));
                return crossReferences;
            }
        }

        return fieldValue;
    }
}
