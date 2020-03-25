package org.uniprot.api.subcell.output;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
public class SubcellularLocationEntryFilter {

    private static String DEFAULT_FIELDS = "id,definition,category,name";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Utils.nullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }

    public static SubcellularLocationEntry filterEntry(
            SubcellularLocationEntry entry, List<String> fields) {
        SubcellularLocationEntryBuilder entryBuilder = SubcellularLocationEntryBuilder.from(entry);
        if (Utils.notNullNotEmpty(fields)) {
            for (SubcellularLocationField.ResultFields field :
                    SubcellularLocationField.ResultFields.values()) {
                if (!fields.contains(field.name())) {
                    remove(entryBuilder, field);
                }
            }
        }
        return entryBuilder.build();
    }

    private static void remove(
            SubcellularLocationEntryBuilder entry, SubcellularLocationField.ResultFields field) {
        switch (field) {
            case id:
                break;
            case definition:
                entry.definition(null);
                break;
            case category:
                entry.category(null);
                break;
            case keyword:
                entry.keyword(null);
                break;
            case synonyms:
                entry.synonymsSet(null);
                break;
            case content:
                entry.content(null);
                break;
            case gene_ontologies:
                entry.geneOntologiesSet(null);
                break;
            case note:
                entry.note(null);
                break;
            case references:
                entry.referencesSet(null);
                break;
            case links:
                entry.linksSet(null);
                break;
            case is_a:
                entry.isASet(null);
                break;
            case part_of:
                entry.partOfSet(null);
                break;
            case statistics:
                entry.statistics(null);
                break;
            case name:
                entry.name(null);
                break;
        }
    }
}
