package uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.EvidenceItem;

@Data
@NoArgsConstructor
public class EvidenceItemImpl implements EvidenceItem {

    private String name;
    private String code;
}
