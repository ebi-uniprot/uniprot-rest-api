package org.uniprot.api.support.data.statistics.entity;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(columnList = "attributeName", unique = true))
public class AttributeQuery {
    @Id private Long id;

    @Column(unique = true)
    private String attributeName;

    private String query;
}
