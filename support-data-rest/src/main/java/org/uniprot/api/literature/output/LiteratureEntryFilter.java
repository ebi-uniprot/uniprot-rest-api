package org.uniprot.api.literature.output;

import static org.uniprot.core.util.Utils.modifiableList;

import java.util.Arrays;
import java.util.List;

import org.uniprot.core.DBCrossReference;
import org.uniprot.core.citation.Author;
import org.uniprot.core.citation.CitationXrefType;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.PublicationDate;
import org.uniprot.core.citation.builder.LiteratureBuilder;
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
        if (Utils.notNullNotEmpty(fields)) {
            for (LiteratureField.ResultFields field : LiteratureField.ResultFields.values()) {
                if (!fields.contains(field.name())) {
                    entity = remove(entity, field);
                }
            }
        }
        return entity;
    }

    private static LiteratureEntry remove(
            LiteratureEntry entity, LiteratureField.ResultFields field) {
        LiteratureEntryBuilder entryBuilder = LiteratureEntryBuilder.from(entity);
        Literature literature = (Literature) entity.getCitation();
        LiteratureBuilder litBuilder = LiteratureBuilder.from(literature);
        switch (field) {
            case id:
            case reference:
            case author_and_group:
                break;
            case doi:
                List<DBCrossReference<CitationXrefType>> xrefs =
                        modifiableList(literature.getCitationXrefs());
                xrefs.removeIf(xref -> xref.getDatabaseType().equals(CitationXrefType.DOI));
                litBuilder.citationXrefsSet(xrefs);
                break;
            case title:
                litBuilder.title(null);
                break;
            case authoring_group:
                litBuilder.authoringGroupsSet(null);
                break;
            case author:
                litBuilder.authorsSet((List<Author>) null);
                break;
            case journal:
                litBuilder.journalName(null);
                break;
            case publication:
                litBuilder.publicationDate((PublicationDate) null);
                break;
            case lit_abstract:
                litBuilder.literatureAbstract(null);
                break;
            case statistics:
                entryBuilder.statistics(null);
                break;
            case first_page:
                litBuilder.firstPage(null);
                break;
            case last_page:
                litBuilder.lastPage(null);
                break;
            case volume:
                litBuilder.volume(null);
                break;
        }
        return entryBuilder.citation(litBuilder.build()).build();
    }
}
