package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.Diff;
import org.uniprot.api.unisave.repository.domain.Entry;

public class DiffImpl implements Diff {

    private String accession;
    private String diff;
    private Entry entryOne;
    private Entry entryTwo;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public Entry getEntryOne() {
        return entryOne;
    }

    public void setEntryOne(Entry entryOne) {
        this.entryOne = entryOne;
    }

    public Entry getEntryTwo() {
        return entryTwo;
    }

    public void setEntryTwo(Entry entryTwo) {
        this.entryTwo = entryTwo;
    }
}
