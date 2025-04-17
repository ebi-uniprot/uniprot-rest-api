package org.uniprot.api.mapto.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.config.UniProtDataType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;

@Data
@RedisHash("mapto")
public class MapToJob implements Serializable {
    @Serial private static final long serialVersionUID = -3919276929568870010L;
    @Id private String id;
    private UniProtDataType sourceDB;
    private UniProtDataType targetDB;
    private String query; // query to run against the source collection
    private JobStatus status;
    private Map<String, String> extraParams;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime created;

    private List<String> targetIds;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updated;
}
