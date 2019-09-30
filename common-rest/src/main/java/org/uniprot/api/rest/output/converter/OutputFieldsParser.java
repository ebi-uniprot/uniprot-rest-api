package org.uniprot.api.rest.output.converter;

import java.util.Arrays;
import java.util.List;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class OutputFieldsParser {
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields, String defaultFields) {
        if ((fields == null) || (fields.isEmpty())) {
            fields = defaultFields;
        }
        return Arrays.asList(fields.split(COMMA));
    }
}
