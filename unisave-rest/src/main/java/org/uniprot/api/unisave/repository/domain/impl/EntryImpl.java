package org.uniprot.api.unisave.repository.domain.impl;

import javax.persistence.*;

import lombok.Data;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Entry;

@Entity(name = "Entry")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accession", "entry_version"}))
@NamedQueries({
    @NamedQuery(
            name = "EntryImpl.findEntryByAccessionAndVersion",
            query = "SELECT e from Entry e WHERE e.accession=:acc AND e.entryVersion=:version"),
    @NamedQuery(
            name = "EntryImpl.findEntriesByAccession",
            query =
                    "SELECT e from Entry e WHERE e.accession=:acc and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"
                            + " ORDER BY e.entryVersion DESC"),
    @NamedQuery(
            name = "EntryImpl.findEntryByAccessionAndEntryId",
            query = "SELECT e from Entry e WHERE e.accession=:acc AND e.entryId=:id"),
    @NamedQuery(
            name = "EntryImpl.findEntryInfoByAccessionAndVersion",
            query =
                    "SELECT e.database, e.accession, e.name, e.entryVersion, e.entryMD5, e.sequenceVersion, e.sequenceMD5, "
                            + "r1, r2 from Entry e left join e.firstRelease r1 left join e.lastRelease r2 WHERE e.accession=:acc AND e.entryVersion=:version  and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"),
    @NamedQuery(
            name = "EntryImpl.findEntryInfosByAccession",
            query =
                    "SELECT e.database, e.accession, e.name, e.entryVersion, e.entryMD5, e.sequenceVersion, e.sequenceMD5, "
                            + "r1, r2 from Entry e left join e.firstRelease r1 left join e.lastRelease r2 WHERE e.accession=:acc and e.database <> org.uniprot.api.unisave.repository.domain.DatabaseEnum.WrongTrembl"
                            + " ORDER BY e.entryVersion DESC"),
})
@Data
public class EntryImpl implements Entry {

    public enum Query {
        findEntryByAccessionAndEntryId,
        findEntryByAccessionAndVersion,
        findEntriesByAccession,
        findEntryInfoByAccessionAndVersion,
        findEntryInfosByAccession;

        public String query() {
            return EntryImpl.class.getSimpleName() + "." + name();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entrySeq")
    @SequenceGenerator(name = "entrySeq", sequenceName = "ENTRY_SEQ", allocationSize = 1)
    @Column(name = "entry_id")
    private long entryId;

    @Column
    @Enumerated(value = EnumType.ORDINAL)
    private DatabaseEnum database;

    @Column(name = "accession", nullable = false)
    private String accession;

    @Column(name = "entry_name", nullable = false)
    private String name;

    @Column(nullable = false, name = "sequence_version")
    private int sequenceVersion;

    @Column(name = "entry_version", nullable = false)
    private int entryVersion;

    @Column(name = "entry_md5", nullable = false)
    private String entryMD5;

    @Column(name = "sequence_md5", nullable = false)
    private String sequenceMD5;

    @Embedded private EntryContentImpl entryContent;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "first_release_id")
    private ReleaseImpl firstRelease;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "last_release_id")
    private ReleaseImpl lastRelease;

    /**
     * The year in which the entry is released. this is mainly for the purpose of partition in
     * oracle, which is based on this column.
     *
     * <p>Value of zero means it is in the latest release. it's value is only updated when the entry
     * is not in the latest release anymore.
     */
    @Column(name = "last_release_year")
    private int releaseYear = 0;

    @Override
    public String toString() {
        return this.getEntryContent().toString();
    }
}
