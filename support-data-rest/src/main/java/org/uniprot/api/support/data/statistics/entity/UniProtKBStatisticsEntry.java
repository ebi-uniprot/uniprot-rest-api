package org.uniprot.api.support.data.statistics.entity;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "uniprotkb_statistics_entry", indexes = @Index(columnList = "release_name"))
public class UniProtKBStatisticsEntry {
    @Id private Long id;
    private String attributeName;

    @ManyToOne
    @JoinColumn(name = "statistics_category_id")
    private StatisticsCategory statisticsCategory;

    private Long valueCount;
    private Long entryCount;
    private String description;

    @ManyToOne
    @JoinColumn(name = "release_name")
    private UniProtRelease uniProtRelease;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;
}
