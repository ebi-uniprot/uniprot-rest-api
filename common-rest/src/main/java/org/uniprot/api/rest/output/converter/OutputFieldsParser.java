package org.uniprot.api.rest.output.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.core.util.Utils;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class OutputFieldsParser {

    public static List<ReturnField> parse(String fields, final ReturnFieldConfig fieldConfig) {
        List<ReturnField> returnFields;
        if (Utils.notNullNotEmpty(fields)) {
            returnFields = new ArrayList<>();
            // add from field as the first field if present for idmapping
            addFromFieldIfPresent(returnFields, fieldConfig);
            returnFields.addAll(
                    Arrays.stream(fields.replaceAll("\\s", "").split(","))
                            .map(fieldConfig::getReturnFieldByName)
                            .collect(Collectors.toList()));

        } else {
            returnFields = fieldConfig.getDefaultReturnFields();
        }
        return returnFields;
    }

    public static List<String> getData(Map<String, String> mappedField, List<ReturnField> fields) {
        return fields.stream()
                .map(ReturnField::getName)
                .map(field -> mappedField.getOrDefault(field, ""))
                .collect(Collectors.toList());
    }

    private static void addFromFieldIfPresent(
            List<ReturnField> returnFields, ReturnFieldConfig fieldConfig) {
        Optional<ReturnField> optFrom =
                fieldConfig.getReturnFields().stream()
                        .filter(rf -> "from".equalsIgnoreCase(rf.getName()))
                        .findFirst();
        if (optFrom.isPresent()) {
            returnFields.add(optFrom.get());
        }
    }
}
