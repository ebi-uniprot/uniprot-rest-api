package org.uniprot.api.statistics.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class UniprotkbStatisticsEntry {
    @Id private Long id;
    private String attributeName;
    @ManyToOne
    @JoinColumn(name = "statistics_category_id")
    private StatisticsCategory statisticsCategory;
    private Long valueCount;
    private Long entryCount;
    private String description;
    private String releaseName;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;
}
