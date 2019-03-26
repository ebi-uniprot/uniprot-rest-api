package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceItem;

@Data
@NoArgsConstructor
public class EvidenceItemImpl implements EvidenceItem {

    private String name;
    private String code;
}
