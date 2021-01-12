package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.literature.LiteratureMappedReference;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.publication.ComputationallyMappedReference;
import org.uniprot.core.publication.MappedReference;
import org.uniprot.core.uniprotkb.UniProtKBReference;
import org.uniprot.core.util.Utils;

import java.util.List;

/**
 * @author Edd
 */
@Data
@Builder
public class PublicationEntry {
    private Citation citation;
    private List<MappedReference> references;
    private Statistics statistics;

    @Data
    @Builder
    public static class Statistics {
        private final long reviewedMappedProteinCount;
        private final long unreviewedMappedProteinCount;
        private final long computationalMappedProteinCount;
        private final long communityMappedProteinCount;
    }
}
