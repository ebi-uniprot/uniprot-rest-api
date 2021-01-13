package org.uniprot.api.uniprotkb.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.uniprot.core.citation.Citation;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.publication.MappedReference;

/** @author Edd */
@Data
@Builder
public class PublicationEntry {
    private Citation citation;
    private List<MappedReference> references;
    private LiteratureStatistics statistics;
}
