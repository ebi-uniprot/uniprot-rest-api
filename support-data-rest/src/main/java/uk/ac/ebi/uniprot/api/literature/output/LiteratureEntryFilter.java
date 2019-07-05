package uk.ac.ebi.uniprot.api.literature.output;

import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
public class LiteratureEntryFilter {

    private static String DEFAULT_FIELDS = "id,title,reference,abstract";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Utils.notEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }

    public static LiteratureEntry filterEntry(LiteratureEntry entity, ArrayList<String> strings) {
        return null;
    }
}
