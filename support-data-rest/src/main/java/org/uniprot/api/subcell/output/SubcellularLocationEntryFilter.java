package org.uniprot.api.subcell.output;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
public class SubcellularLocationEntryFilter {

    private static String DEFAULT_FIELDS = "accession,definition,category,id";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Utils.nullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }

    public static SubcellularLocationEntry filterEntry(
            SubcellularLocationEntry entry, List<String> fields) {
        SubcellularLocationEntryImpl entryImpl = (SubcellularLocationEntryImpl) entry;
        if (Utils.notNullNotEmpty(fields)) {
            for (SubcellularLocationField.ResultFields field :
                    SubcellularLocationField.ResultFields.values()) {
                if (!fields.contains(field.name())) {
                    remove(entryImpl, field);
                }
            }
        }
        return entryImpl;
    }

    private static void remove(
            SubcellularLocationEntryImpl entry, SubcellularLocationField.ResultFields field) {
        switch (field) {
            case id:
            case accession:
                break;
            case definition:
                entry.setDefinition(null);
                break;
            case category:
                entry.setCategory(null);
                break;
            case keyword:
                entry.setKeyword(null);
                break;
            case synonyms:
                entry.setSynonyms(null);
                break;
            case content:
                entry.setContent(null);
                break;
            case gene_ontologies:
                entry.setGeneOntologies(null);
                break;
            case note:
                entry.setNote(null);
                break;
            case references:
                entry.setReferences(null);
                break;
            case links:
                entry.setLinks(null);
                break;
            case is_a:
                entry.setIsA(null);
                break;
            case part_of:
                entry.setPartOf(null);
                break;
            case statistics:
                entry.setStatistics(null);
                break;
        }
    }
}
