package org.uniprot.api.mapto.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.config.UniProtDataType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;

@Data
@Entity
@Table(indexes = @Index(name = "job_id_index", columnList = "jobId"))
public class MapToJob implements Serializable {
    @Serial private static final long serialVersionUID = -3919276929568870010L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String jobId;

    @NotNull private UniProtDataType sourceDB;
    @NotNull private UniProtDataType targetDB;
    @NotNull private String query;
    @NotNull private JobStatus status;
    private Boolean includeIsoform;
    @Embedded private ProblemPair error;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @CreationTimestamp
    private LocalDateTime created;

    @OneToMany(
            mappedBy = "mapToJob",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<MapToResult> targetIds = new ArrayList<>();

    private Long totalTargetIds;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @UpdateTimestamp
    private LocalDateTime updated;
}
