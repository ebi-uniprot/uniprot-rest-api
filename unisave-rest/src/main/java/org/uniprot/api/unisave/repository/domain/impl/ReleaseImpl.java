package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Release;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "Release")
@NamedQueries({
    @NamedQuery(
            name = "ReleaseImpl.findReleaseByReleaseNumber",
            query = "SELECT r from Release r where r.releaseNumber=:rel"),
    @NamedQuery(
            name = "ReleaseImpl.findAllRelease",
            query = "SELECT r from Release r where r.id <> 9999999999 order by r.id desc")
})
@Data
public class ReleaseImpl implements Release {

    public enum Query {
        findReleaseByReleaseNumber,
        findAllRelease;

        public String query() {
            return ReleaseImpl.class.getSimpleName() + "." + name();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "release_seq")
    @SequenceGenerator(name = "release_seq", sequenceName = "RELEASE_SEQ", allocationSize = 1)
    @Column(name = "Release_id")
    private long id;

    @Column(name = "release_number", nullable = false, unique = true)
    private String releaseNumber;

    @Column(name = "release_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date releaseDate;

    @Transient private DatabaseEnum database;
}
