package org.uniprot.api.uniprotkb.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

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

    private UniProtReference reference;

    private LiteratureStatistics statistics;

    private LiteratureMappedReference literatureMappedReference;

    private List<String> categories;

    private String publicationSource;

    public boolean isLargeScale() {
        boolean result = false;
        if (hasStatistics()) {
            LiteratureStatistics stat = getStatistics();
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

    public boolean hasStatistics() {
        return getStatistics() != null;
    }

    public boolean hasLiteratureMappedReference() {
        return getLiteratureMappedReference() != null;
    }

    public boolean hasReference() {
        return getReference() != null;
    }

    public boolean hasCategories() {
        return Utils.notNullNotEmpty(getCategories());
    }

    public boolean hasPublicationSource() {
        return Utils.notNullNotEmpty(publicationSource);
    }
}
