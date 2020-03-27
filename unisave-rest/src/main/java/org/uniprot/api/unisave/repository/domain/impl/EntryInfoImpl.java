package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.EntryInfo;
import org.uniprot.api.unisave.repository.domain.Release;

import java.util.ArrayList;
import java.util.List;

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

    private List<String> replacingAcc = new ArrayList<String>();

    private List<String> mergingTo = new ArrayList<String>();
    private boolean isDeleted =false;

    @Override
    public List<String> getMergingTo() {
        return mergingTo;
    }

    public void setMergingTo(List<String> mergingTo) {
        this.mergingTo.clear();
        this.mergingTo.addAll(mergingTo);
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

	public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public DatabaseEnum getDatabase() {
		return database;
    }


	@Override
	public String getDeletionReason() {
		return this.deletionReason;
	}

	public void setDeletionReason(String deletionReason) {
		this.deletionReason = deletionReason;
	}

	public void setDatabase(DatabaseEnum database) {
		this.database = database;
	}


	public String getAccession() {
		return accession;
	}

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccession(String accession) {
		this.accession = accession;
	}

	public int getSequenceVersion() {
		return sequenceVersion;
	}

	public void setSequenceVersion(int sequenceVersion) {
		this.sequenceVersion = sequenceVersion;
	}

	public int getEntryVersion() {
		return entryVersion;
	}

	public void setEntryVersion(int entryVersion) {
		this.entryVersion = entryVersion;
	}

	public String getEntryMD5() {
		return entryMD5;
	}

	public void setEntryMD5(String entryMD5) {
		this.entryMD5 = entryMD5;
	}

	public String getSequenceMD5() {
		return sequenceMD5;
	}

	public void setSequenceMD5(String sequenceMD5) {
		this.sequenceMD5 = sequenceMD5;
	}

	public Release getFirstRelease() {
		return firstRelease;
	}

	public void setFirstRelease(Release firstRelease) {
		this.firstRelease = firstRelease;
	}

	public Release getLastRelease() {
		return lastRelease;
	}

	public void setLastRelease(Release lastRelease) {
		this.lastRelease = lastRelease;
	}


    @Override
    public List<String> getReplacingAccession() {
        return this.replacingAcc;
    }

    public void setReplacingAccession(List<String> ac){
        this.replacingAcc.clear();
        this.replacingAcc.addAll(ac);
    }

}
