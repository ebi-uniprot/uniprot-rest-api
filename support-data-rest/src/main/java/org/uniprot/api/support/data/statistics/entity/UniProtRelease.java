package org.uniprot.api.support.data.statistics.entity;

import java.util.Date;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "uniprot_release")
public class UniProtRelease {
    @Id private int id;

    @Temporal(TemporalType.DATE)
    private Date date;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    private String name;
}
