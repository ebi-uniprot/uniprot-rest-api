package org.uniprot.api.unisave.repository.domain;

public interface Diff {
    String getAccession();

    Entry getEntryOne();

    Entry getEntryTwo();

    String getDiff();
}
