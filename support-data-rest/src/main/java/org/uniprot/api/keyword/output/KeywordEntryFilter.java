package org.uniprot.api.keyword.output;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.util.Utils;

/** @author lgonzales */
public class KeywordEntryFilter {

    private static String DEFAULT_FIELDS = "id,name,description,category";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Utils.nullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }
}
