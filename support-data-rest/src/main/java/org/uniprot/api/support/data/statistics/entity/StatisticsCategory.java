package org.uniprot.api.support.data.statistics.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class StatisticsCategory {
    @Id
    private Long id;
    private String category;
    private String dbType;
    private String searchField;
    private String label;
}
