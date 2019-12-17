package org.uniprot.api.uniprotkb.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureMappedReference;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.uniprot.UniProtReference;
import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Data
@Builder
public class PublicationEntry {

    private LiteratureEntry literatureEntry;

    private LiteratureMappedReference literatureMappedReference;

    private UniProtReference uniProtReference;

    private List<String> categories;

    private String publicationSource;

    public boolean isLargeScale() {
        boolean result = false;
        if (hasLiteratureEntry() && getLiteratureEntry().hasStatistics()) {
            LiteratureStatistics stat = getLiteratureEntry().getStatistics();
            long referencedProteins =
                    stat.getMappedProteinCount()
                            + stat.getReviewedProteinCount()
                            + stat.getUnreviewedProteinCount();
            if (referencedProteins > 50) {
                result = true;
            }
        }
        return result;
    }

    public boolean hasLiteratureEntry() {
        return getLiteratureEntry() != null;
    }

    public boolean hasLiteratureMappedReference() {
        return getLiteratureMappedReference() != null;
    }

    public boolean hasUniProtReference() {
        return getUniProtReference() != null;
    }

    public boolean hasCategories() {
        return Utils.notNullOrEmpty(getCategories());
    }
}
