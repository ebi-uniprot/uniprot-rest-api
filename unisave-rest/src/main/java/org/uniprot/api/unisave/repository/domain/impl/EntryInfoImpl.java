package org.uniprot.api.unisave.repository.domain.impl;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.EntryInfo;
import org.uniprot.api.unisave.repository.domain.Release;

import lombok.Data;

@Data
public class EntryInfoImpl implements EntryInfo {
    private DatabaseEnum database;
    private String accession;
    private int sequenceVersion;
    private int entryVersion;
    private String entryMD5;
    private String name;
    private String sequenceMD5;
    private Release firstRelease;
    private Release lastRelease;
    private String deletionReason;
    private List<String> replacingAccession = new ArrayList<>();
    private List<String> mergingTo = new ArrayList<>();
    private boolean isDeleted = false;

    public void setReplacingAccession(List<String> ac) {
        this.replacingAccession.clear();
        this.replacingAccession.addAll(ac);
    }
}
