package org.uniprot.api.taxonomy.output;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.builder.TaxonomyEntryBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.TaxonomyField;

public class TaxonomyEntryFilter {

    private static String DEFAULT_FIELDS = "id,mnemonic,scientific_name,common_name,other_names,reviewed,rank,lineage,parent,host";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList( fields.split(COMMA));
    }

    public static TaxonomyEntry filterEntry(TaxonomyEntry entry, List<String> fields) {
        if(Utils.notEmpty(fields)){
            TaxonomyEntryBuilder builder = new TaxonomyEntryBuilder().from(entry);
            builder.hidden(null);
            builder.active(null);
            for(TaxonomyField.ResultFields field : TaxonomyField.ResultFields.values()){
                if(!fields.contains(field.name())){
                    remove(builder,field);
                }
            }
            return builder.build();
        }else {
            return entry;
        }
    }


    private static void remove(TaxonomyEntryBuilder builder, TaxonomyField.ResultFields field) {
        switch (field){
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
                builder.synonyms(null);
                break;
            case other_names:
                builder.otherNames(null);
                break;
            case rank:
                builder.rank(null);
                break;
            case lineage:
                builder.lineage(null);
                break;
            case strain:
                builder.strains(null);
                break;
            case host:
                builder.hosts(null);
                break;
            case link:
                builder.links(null);
                break;
            case statistics:
            case reviewed:
                builder.statistics(null);
                break;
        }
    }
}
