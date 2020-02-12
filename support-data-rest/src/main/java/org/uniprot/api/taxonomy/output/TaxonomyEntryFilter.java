package org.uniprot.api.taxonomy.output;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.builder.TaxonomyEntryBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.TaxonomyField;

import com.google.common.base.Strings;

public class TaxonomyEntryFilter {

    private static String DEFAULT_FIELDS =
            "id,mnemonic,scientific_name,common_name,other_names,reviewed,rank,lineage,parent,host";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }

    public static TaxonomyEntry filterEntry(TaxonomyEntry entry, List<String> fields) {
        if (Utils.notNullNotEmpty(fields)) {
            TaxonomyEntryBuilder builder = TaxonomyEntryBuilder.from(entry);
            builder.hidden(null);
            builder.active(null);
            for (TaxonomyField.ResultFields field : TaxonomyField.ResultFields.values()) {
                if (!fields.contains(field.name())) {
                    remove(builder, field);
                }
            }
            return builder.build();
        } else {
            return entry;
        }
    }

    private static void remove(TaxonomyEntryBuilder builder, TaxonomyField.ResultFields field) {
        switch (field) {
            case id:
                break;
            case parent:
                builder.parentId(null);
                break;
            case mnemonic:
                builder.mnemonic(null);
                break;
            case scientific_name:
                builder.scientificName(null);
                break;
            case common_name:
                builder.commonName(null);
                break;
            case synonym:
                builder.synonymsSet(null);
                break;
            case other_names:
                builder.otherNamesSet(null);
                break;
            case rank:
                builder.rank(null);
                break;
            case lineage:
                builder.lineagesSet(null);
                break;
            case strain:
                builder.strainsSet(null);
                break;
            case host:
                builder.hostsSet(null);
                break;
            case link:
                builder.linksSet(null);
                break;
            case statistics:
            case reviewed:
                builder.statistics(null);
                break;
        }
    }
}
