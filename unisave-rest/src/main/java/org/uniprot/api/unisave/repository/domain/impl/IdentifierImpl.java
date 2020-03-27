package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.*;

import javax.persistence.*;
import java.util.Date;

//@Entity(name = "Identifier")
//@NamedQueries({
//		@NamedQuery(name = "IdentifierImpl.findIdentifierByAccession", query = "SELECT i from Identifier i where i.accession = :acc"),
//		@NamedQuery(name = "IdentifierImpl.findSecondaryAccByAccession", query = "SELECT i.accession, i.removeRelease.releaseNumber from Identifier i where i.mergedTo = :acc"),
//		@NamedQuery(name = "IdentifierImpl.findMD5ByAccession", query = "SELECT i.currentMD5 from Identifier i where i.accession = :acc")})
public class IdentifierImpl implements Identifier {

	public static enum Query {
		findIdentifierByAccession, findMD5ByAccession, findSecondaryAccByAccession;

		public String query() {
			return IdentifierImpl.class.getSimpleName() + "." + this.name();
		}
	}

	@Id
	@Column(nullable = false, unique = true, updatable = false)
	private String accession;

	@Column(name = "Current_Version", nullable = false)
	private int currentVersion;

	@Column(name = "Current_MD5", nullable = false)
	private String currentMD5;

	@ManyToOne(optional = false)
	@JoinColumn(unique = true, nullable = false, name = "Current_Entry_Id")
	private EntryImpl currentEntry;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private IdentifierStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "Has_Sec_Acc", nullable = true)
    private YesNoEnum hasSecondaryAcc;

    /**
       This is to mark the release when the accession is marked as delete, or merged, i.e.,
       when it is removed the current release.
     */
    @ManyToOne(optional = true)
    @JoinColumn(unique = false, nullable = true, name = "Remove_Release_Id")
    private ReleaseImpl removeRelease;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date timeStamp;

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Column(name = "Merged_To")
	private String mergedTo;

	public String getMergedTo() {
		return mergedTo;
	}

	public void setMergedTo(String mergedTo) {
		this.mergedTo = mergedTo;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	@Override
	public String getAccession() {
		return this.accession;
	}

	public void setCurrentVersion(int currentVersion) {
		this.currentVersion = currentVersion;
	}

	public void setStatus(IdentifierStatusEnum status) {
		this.status = status;
	}

	@Override
	public int getCurrentVersion() {
		return this.currentVersion;
	}

	@Override
	public IdentifierStatusEnum getCurrentStatus() {
		return this.status;
	}

	public Entry getCurrentEntry() {
		return currentEntry;
	}

    @Override
    public IdentifierStatusEnum getStatus() {
        return this.status;
    }

     public void setCurrentEntry(Entry e) {
		if (e instanceof EntryImpl) {
			this.currentEntry = (EntryImpl) e;
		}
	}

	@Override
	public String getCurrentMD5() {
		return currentMD5;
	}

	public void setCurrentMD5(String currentMD5) {
		this.currentMD5 = currentMD5;
	}

    @Override
    public boolean hasSecondaryAcc(){
        return (this.hasSecondaryAcc!=null && this.hasSecondaryAcc==YesNoEnum.Y) ;
    }

    @Override
    public Release getRemoveRelease(){
        return this.removeRelease;
    }

}
