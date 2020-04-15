package org.uniprot.api.unisave.repository.domain;

public interface Diff {

    public String getAccession();

    public Entry getEntryOne();

    public Entry getEntryTwo();

    public String getDiff();
}
