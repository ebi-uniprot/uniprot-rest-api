package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Release;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "Release")
@NamedQueries({
    @NamedQuery(
            name = "ReleaseImpl.FIND_PAST_RELEASES_IN_ORDER",
            query =
                    "select r from Release r where r.releaseDate <= :date order by r.releaseDate desc"),
    @NamedQuery(
            name = "ReleaseImpl.FIND_RELEASE_BY_RELEASE_NUMBER",
            query = "SELECT r from Release r where r.releaseNumber=:rel"),
    @NamedQuery(
            name = "ReleaseImpl.FIND_ALL_RELEASES",
            query = "SELECT r from Release r where r.id <> 9999999999 order by r.id desc")
})
@Data
public class ReleaseImpl implements Release {

    public enum Query {
        FIND_RELEASE_BY_RELEASE_NUMBER,
        FIND_PAST_RELEASES_IN_ORDER,
        FIND_ALL_RELEASES;

        public String query() {
            return ReleaseImpl.class.getSimpleName() + "." + name();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "release_seq")
    @SequenceGenerator(name = "release_seq", sequenceName = "RELEASE_SEQ", allocationSize = 1)
    @Column(name = "release_id")
    private long id;

    @Column(name = "release_number", nullable = false, unique = true)
    private String releaseNumber;

    @Column(name = "release_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date releaseDate;

    @Transient private DatabaseEnum database;
}
