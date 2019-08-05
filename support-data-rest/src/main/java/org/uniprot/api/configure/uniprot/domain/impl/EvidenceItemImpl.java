package org.uniprot.api.configure.uniprot.domain.impl;

import org.uniprot.api.configure.uniprot.domain.EvidenceItem;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EvidenceItemImpl implements EvidenceItem {

    private String name;
    private String code;
}
