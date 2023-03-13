package org.uniprot.api.statistics.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class StatisticsCategory {
    @Id private Long id;
    private String category;
    private String dbType;
    private String searchField;
    private String label;
}
