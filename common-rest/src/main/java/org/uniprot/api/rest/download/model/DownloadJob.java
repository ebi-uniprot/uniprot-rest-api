package org.uniprot.api.rest.download.model;

import java.io.Serializable;
import java.sql.Timestamp;

import lombok.Builder;
import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * @author sahmad
 * @created 22/12/2022
 * To list data in redis-client
 * 1. to list all the keys run KEYS * from redis-cli
 * 2. to get the data for a hash run hgetall AsyncDownloadJob:1e8e33be0c54af8ba15db116e2e6c63b26acd7cd
 */
@RedisHash("AsyncDownloadJob")
@Data
@Builder
public class DownloadJob implements Serializable {
    @Id private String id;
    private JobStatus status;
    private Timestamp created;
    private Timestamp updated;
    private String error;
    private String result;
    private int retried;
    private String query;
    private String fields;
    private String sort;
}
