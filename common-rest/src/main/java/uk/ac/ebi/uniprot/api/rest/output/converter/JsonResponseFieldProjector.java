package uk.ac.ebi.uniprot.api.rest.output.converter;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.domain.uniprot.comment.Comment;
import uk.ac.ebi.uniprot.domain.uniprot.feature.Feature;
import uk.ac.ebi.uniprot.domain.uniprot.xdb.UniProtDBCrossReference;
import uk.ac.ebi.uniprot.search.field.ReturnField;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author sahmad
 * <p>
 * Returns a map of root level fields with their values for the json writer
 */
@Slf4j
public class JsonResponseFieldProjector {
    private static final String ALL = "all";

    // Get a map containing the object's projected fields
    public Map<String, Object> project(Object entry, Map<String, List<String>> filterFieldMap, List<ReturnField> allFields) {

        Map<String, List<String>> camelCaseFieldNameValuesMap;// map to keep java field name with its values if any

        if (!Utils.notEmpty(filterFieldMap)) { // if the filter field map is empty return all fields from allFields
            camelCaseFieldNameValuesMap = allFields
                    .stream()
                    .filter(f -> f.getJavaFieldName() != null)
                    .collect(
                            Collectors.toMap(ReturnField::getJavaFieldName, f -> Collections.emptyList(), (oldKey, newKey) -> oldKey)
                    );
        } else {
            camelCaseFieldNameValuesMap = allFields
                    .stream()
                    .filter(f -> (filterFieldMap.containsKey(f.toString()) || f.isMandatoryJsonField()) && f.getJavaFieldName() != null)
                    .collect(
                            Collectors.toMap(
                                            ReturnField::getJavaFieldName,
                                            f -> filterFieldMap.getOrDefault(f.toString(), Collections.emptyList()),
                                            (oldKey, newKey) -> oldKey
                                            )
                            );

        }

        // create the Map instance to be returned with root fields populated
        Map<String, Object> projectedMap = buildMapWithFields(entry, camelCaseFieldNameValuesMap);

        return projectedMap;

    }

    // Get a map with provided field names and values
    private Map<String, Object> buildMapWithFields(Object source, Map<String, List<String>> camelCaseFieldNameValuesMap) {

        Map<String, Object> target = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : camelCaseFieldNameValuesMap.entrySet()) {
            String javaFieldName = entry.getKey();
            List<String> values = entry.getValue();// values to be returned e.g. comment with type function and domain
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
                Object fieldValue = sourceField.get(source); // extract the value of sourceField from source object
                if (source instanceof UniProtEntry) { // check the values for UniProtEntry
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
        } catch (NoSuchFieldException nsf) { // go up the hierarchy to see if the field is defined in parent class
            if (source.getSuperclass() != Object.class) {
                field = getField(source.getSuperclass(), fieldName);
            } else {
                log.warn("{} not found", fieldName);// incorrect fieldName provided
            }
        }

        return field;
    }

    private Object getObjectsWithValues(Object fieldValue, List<String> neededFieldValues) {
        if (Utils.notEmpty(neededFieldValues) && fieldValue != null
                && fieldValue instanceof List<?> && Utils.notEmpty((List<?>) fieldValue)) {
            // comment
            if (((List<?>) fieldValue).get(0) instanceof Comment) { // check one to decide the type of list items
                Predicate<Comment> filter = EntryFilters.createCommentFilter(neededFieldValues);
                List<Comment> comments = (List<Comment>) fieldValue;
                comments.removeIf(comment -> !filter.test(comment));
                return comments;
            } else if (((List<?>) fieldValue).get(0) instanceof Feature) { // feature
                Predicate<Feature> filter = EntryFilters.createFeatureFilter(neededFieldValues);
                List<Feature> features = (List<Feature>) fieldValue;
                features.removeIf(feature -> !filter.test(feature));
                return features;

            } else if (((List<?>) fieldValue).get(0) instanceof UniProtDBCrossReference) { // cross ref
                Predicate<UniProtDBCrossReference> filter = EntryFilters.createDbReferenceFilter(neededFieldValues);
                List<UniProtDBCrossReference> crossReferences = (List<UniProtDBCrossReference>) fieldValue;
                crossReferences.removeIf(xref -> !filter.test(xref));
                return crossReferences;
            }
        }

        return fieldValue;
    }
}