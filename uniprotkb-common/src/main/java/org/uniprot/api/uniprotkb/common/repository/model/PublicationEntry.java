package org.uniprot.api.uniprotkb.common.repository.model;

import java.util.List;

import org.uniprot.core.citation.Citation;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.publication.MappedReference;

import lombok.Builder;
import lombok.Data;

/**
 * @author Edd
 */
@Data
@Builder
public class PublicationEntry {
    private Citation citation;
    private List<MappedReference> references;
    private LiteratureStatistics statistics;
}
