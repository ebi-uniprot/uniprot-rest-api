package org.uniprot.api.mapto.common.model;

import java.io.Serializable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(indexes = @Index(columnList = "map_to_job_id"))
@AllArgsConstructor
@NoArgsConstructor
public class MapToResult implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "map_to_job_id", nullable = false)
    private MapToJob mapToJob;

    @NotNull private String targetId;

    public MapToResult(MapToJob mapToJob, String targetId) {
        this.mapToJob = mapToJob;
        this.targetId = targetId;
    }
}
