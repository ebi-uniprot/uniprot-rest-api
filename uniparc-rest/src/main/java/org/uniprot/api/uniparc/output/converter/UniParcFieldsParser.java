package org.uniprot.api.uniparc.output.converter;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Strings;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class UniParcFieldsParser {
    private static String DEFAULT_FIELDS = "upid,organism,organism_id,protein_count";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }
}
