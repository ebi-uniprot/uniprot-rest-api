package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.Diff;
import org.uniprot.api.unisave.repository.domain.Entry;

import lombok.Data;

@Data
public class DiffImpl implements Diff {
    private String accession;
    private String diff;
    private Entry entryOne;
    private Entry entryTwo;
}
