package org.uniprot.api.rest.download.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
 * @author sahmad
 * @created 22/12/2022 To list data in redis-client 1. to list all the keys run KEYS * from
 *     redis-cli 2. to get the data for a hash run hgetall
 *     AsyncDownloadJob:1e8e33be0c54af8ba15db116e2e6c63b26acd7cd
 */
@RedisHash("AsyncDownloadJob")
@Data
@Builder
public class DownloadJob implements Serializable {
    @Id private String id;
    private JobStatus status;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime created;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updated;

    private String error;
    private int retried;
    private String query;
    private String fields;
    private String sort;
    private String resultFile;

    private String format;
}