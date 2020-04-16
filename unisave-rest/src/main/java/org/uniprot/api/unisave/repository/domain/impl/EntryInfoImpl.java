package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.EntryInfo;
import org.uniprot.api.unisave.repository.domain.Release;

import java.util.ArrayList;
import java.util.List;

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

//    public void setMergingTo(List<String> mergingTo) {
//        this.mergingTo.clear();
//        this.mergingTo.addAll(mergingTo);
//    }

    public void setReplacingAccession(List<String> ac) {
        this.replacingAccession.clear();
        this.replacingAccession.addAll(ac);
    }
}
