package org.uniprot.api.support.data.statistics.entity;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "attribute_query", indexes = @Index(columnList = "attributeName, statistics_category_id", unique = true))
public class AttributeQuery {
    @Id private Long id;

    @ManyToOne
    @JoinColumn(name = "statistics_category_id")
    private StatisticsCategory statisticsCategory;

    private String attributeName;

    private String query;
}
