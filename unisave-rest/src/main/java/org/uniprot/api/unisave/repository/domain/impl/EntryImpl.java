package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Entry;

import javax.persistence.*;

// org.uniprot.api.unisave.repository.domain
// org.uniprot.api.unisave.repository.domain.DatabaseEnum
@Entity(name = "Entry")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accession", "entry_Version"}))
@NamedQueries({
    @NamedQuery(
            name = "EntryImpl.findEntryByAccessionAndVersion",
            query = "SELECT e from Entry e WHERE e.accession=:acc AND e.entryVersion=:version"),
    @NamedQuery(
            name = "EntryImpl.findEntryByAccessionAndRelease",
            query =
                    "SELECT e from Entry e WHERE e.accession=:acc and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"
                            + " AND e.firstRelease.releaseDate<=:rel and e.lastRelease.releaseDate>=:rel"),
    @NamedQuery(
            name = "EntryImpl.findEntriesByAccession",
            query =
                    "SELECT e from Entry e WHERE e.accession=:acc and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"
                            + " ORDER BY e.entryVersion DESC"),
    @NamedQuery(
            name = "EntryImpl.findEntriesByAccessionAndDb",
            query =
                    "SELECT e from Entry e WHERE e.accession=:acc and e.database =:db ORDER BY e.entryVersion DESC"),
    @NamedQuery(
            name = "EntryImpl.findEntryByAccessionAndEntryId",
            query = "SELECT e from Entry e WHERE e.accession=:acc AND e.entryid=:id"),

    // @NamedQuery(name = "EntryImpl.findEntryInfoByAccessionAndVersion",
    // query =
    // "SELECT e.database, e.accession, e.entryVersion, e.md5, e.sequenceVersion, e.sequenceMD5,
    // e.firstRelease, "
    // +
    // "e.lastRelease from Entry e WHERE e.accession=:acc AND e.entryVersion=:version"),

    @NamedQuery(
            name = "EntryImpl.findEntryInfoByAccessionAndVersion",
            query =
                    "SELECT e.database, e.accession, e.name, e.entryVersion, e.entryMD5, e.sequenceVersion, e.sequenceMD5, "
                            + "r1, r2 from Entry e left join e.firstRelease r1 left join e.lastRelease r2 WHERE e.accession=:acc AND e.entryVersion=:version  and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"),

    // @NamedQuery(name = "EntryImpl.findEntryInfoByAccessionAndRelease",
    // query =
    // "SELECT e.database, e.accession, e.entryVersion, e.md5, e.sequenceVersion, e.sequenceMD5,
    // e.firstRelease, "
    // +
    // "e.lastRelease from Entry e WHERE e.accession=:acc AND e.firstRelease.releaseDate<=:rel and
    // e.lastRelease.releaseDate>=:rel"),

    @NamedQuery(
            name = "EntryImpl.findEntryInfoByAccessionAndRelease",
            query =
                    "SELECT e.database, e.accession, e.name, e.entryVersion, e.entryMD5, e.sequenceVersion, e.sequenceMD5, "
                            + "r1, r2  from Entry e left join e.firstRelease r1 left join e.lastRelease r2 WHERE e.accession=:acc AND r1.releaseDate<=:rel and r2.releaseDate>=:rel "
                            + "  and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"),
    @NamedQuery(
            name = "EntryImpl.findEntryInfosByAccession",
            query =
                    "SELECT e.database, e.accession, e.name, e.entryVersion, e.entryMD5, e.sequenceVersion, e.sequenceMD5, "
                            + "r1, r2 from Entry e left join e.firstRelease r1 left join e.lastRelease r2 WHERE e.accession=:acc and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"
                            + " ORDER BY e.entryVersion DESC"),
    @NamedQuery(
            name = "EntryImpl.findEntryInfosByAccessionAndDatabase",
            query =
                    "SELECT e.database, e.accession, e.name, e.entryVersion, e.entryMD5, e.sequenceVersion, e.sequenceMD5, "
                            + "r1, r2 from Entry e left join e.firstRelease r1 left join e.lastRelease r2 WHERE e.accession=:acc and e.database =:db ORDER BY e.entryVersion DESC")
})
@Data
public class EntryImpl implements Entry {

    public enum Query {
        findEntryByAccessionAndEntryId,
        findEntryByAccessionAndVersion,
        findEntryByAccessionAndRelease,
        findEntriesByAccession,
        findEntriesByAccessionAndDb,
        findEntryInfoByAccessionAndVersion,
        findEntryInfoByAccessionAndRelease,
        findEntryInfosByAccession,
        findEntryInfosByAccessionAndDatabase;

        public String query() {
            return EntryImpl.class.getSimpleName() + "." + name();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entrySeq")
    @SequenceGenerator(name = "entrySeq", sequenceName = "ENTRY_SEQ", allocationSize = 1)
    @Column(name = "entry_id")
    private long entryid;

    public long getEntryid() {
        return entryid;
    }

    @Column
    @Enumerated(value = EnumType.ORDINAL)
    private DatabaseEnum database;

    @Column(name = "accession", nullable = false)
    private String accession;

    @Column(name = "entry_name", nullable = false)
    private String name;

    @Column(nullable = false, name = "sequence_Version")
    private int sequenceVersion;

    @Column(name = "entry_Version", nullable = false)
    private int entryVersion;

    @Column(name = "entry_MD5", nullable = false)
    private String entryMD5;

    @Column(name = "sequence_md5", nullable = false)
    private String sequenceMD5;

    @Embedded private EntryContentImpl entryContent;

    @ManyToOne
    @JoinColumn(name = "First_Release_Id")
    private ReleaseImpl firstRelease;

    @ManyToOne
    @JoinColumn(name = "Last_Release_Id")
    private ReleaseImpl lastRelease;

    /**
     * The year in which the entry is released. this is mainly for the purpose of partition in
     * oracle, which is based on this column.
     *
     * <p>Value of zero means it is in the latest release. it's value is only updated when the entry
     * is not in the latest release anymore.
     */
    @Column(name = "last_release_Year")
    private int release_year = 0;

//    public int getRelease_year() {
//        return release_year;
//    }
//
//    public void setRelease_year(int release_year) {
//        this.release_year = release_year;
//    }
//
//    @Override
//    public Release getFirstRelease() {
//        return firstRelease;
//    }
//
//    public void setFirstRelease(Release release) {
//        if (release instanceof ReleaseImpl) this.firstRelease = (ReleaseImpl) release;
//    }
//
//    @Override
//    public Release getLastRelease() {
//        return lastRelease;
//    }
//
//    public void setLastRelease(Release release) {
//        if (release instanceof ReleaseImpl) this.lastRelease = (ReleaseImpl) release;
//    }
//
//    public void setDatabase(DatabaseEnum database) {
//        this.database = database;
//    }
//
//    public void setAccession(String identifier) {
//        this.accession = identifier;
//    }
//
//    public void setSequenceVersion(int sequenceVersion) {
//        this.sequenceVersion = sequenceVersion;
//    }
//
//    public void setEntryVersion(int entryVersion) {
//        this.entryVersion = entryVersion;
//    }
//
//    public void setEntryMD5(String entryMD5) {
//        this.entryMD5 = entryMD5;
//    }
//
//    public void setSequenceMD5(String sequenceMD5) {
//        this.sequenceMD5 = sequenceMD5;
//    }
//
//    public void setEntryContent(EntryContent entryContent) {
//        this.entryContent = (EntryContentImpl) entryContent;
//    }
//
//    @Override
//    public DatabaseEnum getDatabase() {
//        return database;
//    }
//
//    @Override
//    public String getAccession() {
//        return accession;
//    }
//
//    @Override
//    public int getSequenceVersion() {
//        return sequenceVersion;
//    }
//
//    @Override
//    public int getEntryVersion() {
//        return entryVersion;
//    }
//
//    @Override
//    public String getEntryMD5() {
//        return entryMD5;
//    }
//
//    @Override
//    public String getSequenceMD5() {
//        return sequenceMD5;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    @Override
//    public EntryContent getEntryContent() {
//        return entryContent;
//    }
//
//    public String toCsvString() {
//        return null;
//    }

    @Override
    public String toString() {
        return this.getEntryContent().toString();
    }
}
