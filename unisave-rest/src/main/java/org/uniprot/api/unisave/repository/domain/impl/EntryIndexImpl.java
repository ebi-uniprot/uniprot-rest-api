package org.uniprot.api.unisave.repository.domain.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.EntryIndex;
import org.uniprot.api.unisave.repository.domain.EntryIndexStatus;

import com.google.common.base.MoreObjects;

@Entity(name = "EntryIndex")
@Table(name = "FF_ENTRY_SUMMARY")
// @NamedQueries({@NamedQuery(name = "EntryIndexImpl.truncateTable", query = "DELETE FROM EntryIndex
// ind")})
public class EntryIndexImpl implements EntryIndex {

    public static enum Query {
        findToLoad,
        truncateTable;

        public String query() {
            return EntryIndexImpl.class.getSimpleName() + "." + this.name();
        }
    }

    // @Id
    // @GeneratedValue(strategy = GenerationType.SEQUENCE,
    // generator = "ff_entry_summary_seq")
    // @SequenceGenerator(name = "ff_entry_summary_seq",
    // sequenceName = "FF_ENTRY_SUMMARY_SEQ", allocationSize=1)
    // @Column(name = "FF_ENTRY_SUMMARY_ID")
    // private long id;

    @Column(nullable = false, name = "start_position")
    private long startPosition;

    @Column(nullable = false)
    private int length;

    @Id
    @Column(unique = true)
    private String accession;

    @Column(name = "database_id")
    @Enumerated(EnumType.ORDINAL)
    private DatabaseEnum db;

    @Column(nullable = false)
    private String md5;

    @Column(nullable = false)
    private int version;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "SEC_ACC", nullable = false)
    @CollectionTable(
            name = "FF_ENTRY_SUMMARY_SEC_ACC",
            joinColumns = {@JoinColumn(name = "accession")})
    @Transient
    private List<String> secondaryAcc = new ArrayList<>();

    /** The default is set to null, which means it need update. */
    @Column
    @Enumerated(EnumType.STRING)
    private EntryIndexStatus status = null;

    public EntryIndexStatus getStatus() {
        return status;
    }

    public void setStatus(EntryIndexStatus status) {
        this.status = status;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.uniprot.api.unisave.repository.domain.impl.EntryIndex#getStartPosition()
     */
    @Override
    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.uniprot.api.unisave.repository.domain.impl.EntryIndex#getLength()
     */
    @Override
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.uniprot.api.unisave.repository.domain.impl.EntryIndex#getAcc()
     */
    @Override
    public String getAccession() {
        return accession;
    }

    public void setAccession(String acc) {
        this.accession = acc;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.uniprot.api.unisave.repository.domain.impl.EntryIndex#getDb()
     */
    @Override
    public DatabaseEnum getDb() {
        return db;
    }

    public void setDb(DatabaseEnum db) {
        this.db = db;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.uniprot.api.unisave.repository.domain.impl.EntryIndex#getMd5()
     */
    @Override
    public String getMd5() {
        return md5;
    }

    public void setMD5(String md5) {
        this.md5 = md5;
    }

    @Override
    public List<String> getSecAccs() {
        return this.secondaryAcc;
    }

    public void addSecAcc(String secacc) {
        if (!this.secondaryAcc.contains(secacc)) {
            this.secondaryAcc.add(secacc);
        } else {
            // should print some warning.
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Accession", this.accession)
                .add("MD5", this.md5)
                .add("SecAcc", this.secondaryAcc)
                .add("Start", this.startPosition)
                .add("Length", this.length)
                .toString();
    }

    /** Convert the object into a format compatiable to the SQL*Loader. */
    @Override
    public String toCSVString() {
        StringBuffer sb = new StringBuffer();

        String format =
                String.format(
                        "%1d%6s%32s%10d%15d%n",
                        1, this.accession, this.md5, this.length, this.startPosition);
        sb.append(format);

        for (String s : this.secondaryAcc) {
            String format2 = String.format("%1d%6s%n", 2, s);
            sb.append(format2);
        }

        return sb.toString();
    }
}
