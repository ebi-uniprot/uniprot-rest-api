package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.Diff;
import org.uniprot.api.unisave.repository.domain.Entry;

@Data
public class DiffImpl implements Diff {
    private String accession;
    private String diff;
    private Entry entryOne;
    private Entry entryTwo;
}
