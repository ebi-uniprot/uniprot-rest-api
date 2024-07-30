package org.uniprot.api.support.data.statistics.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(indexes = @Index(columnList = "attributeName", unique = true))
public class AttributeQuery {
    @Id
    private Long id;
    @Column(unique=true)
    private String attributeName;
    private String query;
}
