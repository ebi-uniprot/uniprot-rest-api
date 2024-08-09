package org.uniprot.api.support.data.statistics.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "uniprot_release")
public class UniProtRelease {
    @Id private String id;
    private Date date;
}
