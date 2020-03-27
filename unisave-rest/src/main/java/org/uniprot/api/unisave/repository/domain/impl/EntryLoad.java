package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.repository.domain.EntryContent;
import org.uniprot.api.unisave.repository.domain.Release;

import javax.persistence.*;

@Entity(name = "Entry_LOAD")
public class EntryLoad implements Entry {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE,
			generator = "entry_seq")
	@SequenceGenerator(name = "entry_seq",
			sequenceName = "ENTRY_SEQ", allocationSize = 1)
	@Column(name = "entry_id")
	private long entryid;

	public long getEntryid() {
		return entryid;
	}

    @Column(name = "entry_name", nullable = false)
    private String name;

    @Column
	@Enumerated(value = EnumType.ORDINAL)
	private DatabaseEnum database;

	@Column(name = "accession", nullable = false)
	private String accession;

	@Column(nullable = false, name = "sequence_Version")
	private int sequenceVersion;

	@Column(name = "entry_Version", nullable = false)
	private int entryVersion;

	@Column(name = "entry_MD5", nullable = false)
	private String entryMD5;

	@Column(name = "sequence_md5", nullable = false)
	private String sequenceMD5;

	@Embedded
	private EntryContentImpl entryContent;

	@ManyToOne
	@JoinColumn(name = "First_Release_Id")
	private ReleaseImpl firstRelease;

	@ManyToOne
	@JoinColumn(name = "Last_Release_Id")
	private ReleaseImpl lastRelease;

//	@Column
//	@Temporal(TemporalType.TIMESTAMP)
//	private Date timeStamp = new Date();

	/**
	 * The year in which the entry is released. this is mainly for the purpose
	 * of partition in oracle, which is based on this column.
	 * <p/>
	 * Value of zero means it is in the latest release. it's value is only
	 * updated when the entry is not in the latest release anymore.
	 */
	@Column(name = "last_release_Year")
	private int release_year = 0;

	public EntryLoad(){}

	public EntryLoad(EntryImpl e) {
		this.accession = e.getAccession();
		this.database = e.getDatabase();
		this.entryContent = (EntryContentImpl) e.getEntryContent();

		/*
				new EntryContentImpl();
		if (e.getEntryContent().getType() == ContentTypeEnum.Diff) {
			this.entryContent.setDiffcontent(e.getEntryContent().getDiffcontent(), e.getEntryContent().getReferenceEntryId());
		} else {
			this.entryContent.setFullcontent(e.getEntryContent().getFullcontent());
		} */

		this.entryMD5 = e.getEntryMD5();
		this.entryVersion=e.getEntryVersion();
		this.sequenceMD5=e.getSequenceMD5();
		this.sequenceVersion=e.getSequenceVersion();
		this.firstRelease= (ReleaseImpl) e.getFirstRelease();
		this.lastRelease= (ReleaseImpl) e.getLastRelease();
		this.release_year = e.getRelease_year();
	}

	public int getRelease_year() {
		return release_year;
	}

	public void setRelease_year(int release_year) {
		this.release_year = release_year;
	}

//	public Date getTimeStamp() {
//		return timeStamp;
//	}
//
//	public void setTimeStamp(Date timeStamp) {
//		this.timeStamp = timeStamp;
//	}

	@Override
	public Release getFirstRelease() {
		return firstRelease;
	}

	public void setFirstRelease(Release release) {
		if (release instanceof ReleaseImpl)
			this.firstRelease = (ReleaseImpl) release;
	}

	@Override
	public Release getLastRelease() {
		return lastRelease;
	}

	public void setLastRelease(Release release) {
		if (release instanceof ReleaseImpl)
			this.lastRelease = (ReleaseImpl) release;
	}


	public void setDatabase(DatabaseEnum database) {
		this.database = database;
	}

	public void setAccession(String identifier) {
		this.accession = identifier;
	}

	public void setSequenceVersion(int sequenceVersion) {
		this.sequenceVersion = sequenceVersion;
	}

	public void setEntryVersion(int entryVersion) {
		this.entryVersion = entryVersion;
	}

	public void setEntryMD5(String entryMD5) {
		this.entryMD5 = entryMD5;
	}

	public void setSequenceMD5(String sequenceMD5) {
		this.sequenceMD5 = sequenceMD5;
	}

	public void setEntryContent(EntryContent entryContent) {
		this.entryContent = (EntryContentImpl) entryContent;
	}

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
	public DatabaseEnum getDatabase() {
		return database;
	}

	@Override
	public String getAccession() {
		return accession;
	}

	@Override
	public int getSequenceVersion() {
		return sequenceVersion;
	}

	@Override
	public int getEntryVersion() {
		return entryVersion;
	}

	@Override
	public String getEntryMD5() {
		return entryMD5;
	}

	@Override
	public String getSequenceMD5() {
		return sequenceMD5;
	}

	@Override
	public EntryContent getEntryContent() {
		return entryContent;
	}

	public String toCsvString() {
		return null;
	}

	@Override
	public String toString() {
		return this.getEntryContent().toString();
	}

}
