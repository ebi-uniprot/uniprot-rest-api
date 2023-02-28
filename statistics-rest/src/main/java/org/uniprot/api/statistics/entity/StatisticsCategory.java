package org.uniprot.api.statistics.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class StatisticsCategory {
    @Id private Long id;
    private String category;
    private String dbType;
    private String searchField;
    private String label;
}
