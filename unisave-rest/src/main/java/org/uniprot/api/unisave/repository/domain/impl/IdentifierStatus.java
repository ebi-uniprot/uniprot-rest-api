package org.uniprot.api.unisave.repository.domain.impl;

import javax.persistence.*;

import org.uniprot.api.unisave.repository.domain.AccessionEvent;
import org.uniprot.api.unisave.repository.domain.EventTypeEnum;
import org.uniprot.api.unisave.repository.domain.Release;

/**
 * Created with IntelliJ IDEA. User: wudong Date: 12/11/2013 Time: 14:24 To change this template use
 * File | Settings | File Templates.
 */
@Entity(name = "IdentifierStatus")
@IdClass(IdentifierStatusId.class)
@Table(name = "DELETED_MERGED_ACCESSION_VIEW")
@NamedQueries({
    @NamedQuery(
            name = "IdentifierStatus.findByFirstColumn",
            query = "select i from IdentifierStatus i where i.firstColumn =:acc")
})
public class IdentifierStatus implements AccessionEvent {

    public static enum Query {
        findByFirstColumn;

        public String query() {
            return IdentifierStatus.class.getSimpleName() + "." + this.name();
        }
    }

    @Id
    @Column(name = "OPERATION", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventTypeEnum getType;

    @Id
    @Column(name = "ACCESSION_SUBJECT", nullable = false)
    private String firstColumn;

    @Id
    @Column(name = "ACCESSION_OBJECT")
    private String secondColumn;

    // When this status change happen.
    @ManyToOne
    @JoinColumn(name = "RELEASE_ID")
    private ReleaseImpl release;

    @Column(name = "WITHDRAWN")
    private String withdrawn_flag;

    @Column(name = "DELETION_REASON")
    private String deletion_reason;

    public String getDeletion_reason() {
        return deletion_reason;
    }

    @Override
    public EventTypeEnum getEventType() {
        return getType;
    }

    @Override
    public String getTargetAcc() {
        return secondColumn;
    }

    public String getSourceAcc() {
        return this.firstColumn;
    }

    public boolean isWithdrawn() {
        return withdrawn_flag != null;
    }

    @Override
    public Release getEventRelease() {
        return release;
    }
}
