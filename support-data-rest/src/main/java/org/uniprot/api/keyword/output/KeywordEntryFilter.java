package org.uniprot.api.keyword.output;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.uniprot.core.util.Utils;
import org.uniprot.cv.keyword.KeywordEntry;
import org.uniprot.cv.keyword.impl.KeywordEntryImpl;
import org.uniprot.store.search.field.KeywordField;

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

    public static KeywordEntry filterEntry(KeywordEntry entry, List<String> fields) {
        KeywordEntryImpl entryImpl = (KeywordEntryImpl) entry;
        if (Utils.notNullNotEmpty(fields)) {
            for (KeywordField.ResultFields field : KeywordField.ResultFields.values()) {
                if (!fields.contains(field.name())) {
                    remove(entryImpl, field);
                }
            }
        }
        return entryImpl;
    }

    private static void remove(KeywordEntryImpl entry, KeywordField.ResultFields field) {
        switch (field) {
            case id:
                break;
            case name:
                break;
            case description:
                entry.setDefinition(null);
                break;
            case category:
                entry.setCategory(null);
                break;
            case synonym:
                entry.setSynonyms(Collections.emptyList());
                break;
            case gene_ontology:
                entry.setGeneOntologies(Collections.emptyList());
                break;
            case sites:
                entry.setSites(Collections.emptyList());
                break;
            case children:
                entry.setChildren(Collections.emptyList());
                break;
            case parent:
                entry.setParents(Collections.emptySet());
                break;
            case statistics:
                entry.setStatistics(null);
                break;
        }
    }
}
