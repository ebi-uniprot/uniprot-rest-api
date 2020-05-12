package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class DiseaseComment {
    private String diseaseId;
    private String acronym;
    private DbReference dbReference;
    private EvidencedString description;

    private List<EvidencedString> text;
}
