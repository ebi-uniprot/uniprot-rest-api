package org.uniprot.api.mapto.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.uniprot.api.rest.download.model.JobStatus;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@RedisHash("mapto")
public class MapToJob implements Serializable {
    private static final long serialVersionUID = -3919276929568870010L;
    @Id private String id;
    private String query; // query to run against the source collection
    private String sourceDB;
    private String targetDB;
    private List<String> targetIds;
    private JobStatus status;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime created;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updated;
}
