package org.uniprot.api.support.data.statistics.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "uniprotkb_statistics_entry")
public class UniprotKBStatisticsEntry {
    @Id
    private Long id;
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
