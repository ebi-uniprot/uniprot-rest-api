package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Release;
import org.uniprot.api.unisave.repository.domain.ReleaseStats;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "Release")
@NamedQueries({
    @NamedQuery(name = "ReleaseImpl.findReleaseByReleaseNumber", query = "SELECT r from Release r where r.releaseNumber=:rel"),
    @NamedQuery(name = "ReleaseImpl.findAllRelease", query = "SELECT r from Release r where r.releaseId <> 9999999999 order by r.releaseId desc")
})
public class ReleaseImpl implements Release {

	public static enum Query {
		findReleaseByReleaseNumber, findAllRelease;

		public String query() {
			return ReleaseImpl.class.getSimpleName() + "." + name();
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE,
			generator = "release_seq")
	@SequenceGenerator(name = "release_seq",
			sequenceName = "RELEASE_SEQ",  allocationSize=1)
	@Column(name = "Release_id")
	private long releaseId;

	@Column(name = "release_number", nullable = false, unique = true)
	private String releaseNumber;

	@Column(name = "release_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date releaseDate;

	//@Column(name = "database_id")
	//@Enumerated(EnumType.ORDINAL)
	@Transient
	private DatabaseEnum database;

	/**
	 * @Column(name = "release_uri") private String releaseURI;
	 */

	//@Column
	//@Temporal(TemporalType.TIMESTAMP)
	@Transient
	private Date timeStamp;

	@Transient
	private ReleaseStats stats;

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public String getReleaseNumber() {
		return this.releaseNumber;
	}

	public long getId() {
		return this.releaseId;
	}

	@Override
	public Date getReleaseDate() {
		return this.releaseDate;
	}

	@Override
	public String getReleaseURI() {
		//return this.releaseURI;
		return null;
	}

	public void setId(long id) {
		this.releaseId = id;
	}

	public void setReleaseNumber(String releaseNumber) {
		this.releaseNumber = releaseNumber;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public void setReleaseURI(String releaseURI) {
		//this.releaseURI = releaseURI;
	}

	@Override
	public DatabaseEnum getDatabase() {
		return this.database;
	}

	public void setDatabase(DatabaseEnum database) {
		this.database = database;
	}

	@Override
	public ReleaseStats getStatus() {
		return this.stats;
	}

}
