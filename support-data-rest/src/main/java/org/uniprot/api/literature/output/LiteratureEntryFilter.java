package org.uniprot.api.literature.output;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.citation.Journal;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
public class LiteratureEntryFilter {

    private static String DEFAULT_FIELDS = "id,title,reference,lit_abstract";
    private static final String COMMA = "\\s*,\\s*";

    public static List<String> parse(String fields) {
        if (Utils.nullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        }
        return Arrays.asList(fields.split(COMMA));
    }

    public static LiteratureEntry filterEntry(LiteratureEntry entity, List<String> fields) {
        LiteratureEntryBuilder entryBuilder = LiteratureEntryBuilder.from(entity);
        if (Utils.notNullNotEmpty(fields)) {
            for (LiteratureField.ResultFields field : LiteratureField.ResultFields.values()) {
                if (!fields.contains(field.name())) {
                    remove(entryBuilder, field);
                }
            }
        }
        return entryBuilder.build();
    }

    private static void remove(
            LiteratureEntryBuilder entryBuilder, LiteratureField.ResultFields field) {
        switch (field) {
            case id:
            case reference:
            case author_and_group:
                break;
            case doi:
                entryBuilder.doiId(null);
                break;
            case title:
                entryBuilder.title(null);
                break;
            case authoring_group:
                entryBuilder.authoringGroupsSet(null);
                break;
            case author:
                entryBuilder.authorsSet(null);
                break;
            case journal:
                entryBuilder.journal((Journal) null);
                break;
            case publication:
                entryBuilder.publicationDate(null);
                break;
            case lit_abstract:
                entryBuilder.literatureAbstract(null);
                break;
            case statistics:
                entryBuilder.statistics(null);
                break;
            case first_page:
                entryBuilder.firstPage(null);
                break;
            case last_page:
                entryBuilder.lastPage(null);
                break;
            case volume:
                entryBuilder.volume(null);
                break;
        }
    }
}
