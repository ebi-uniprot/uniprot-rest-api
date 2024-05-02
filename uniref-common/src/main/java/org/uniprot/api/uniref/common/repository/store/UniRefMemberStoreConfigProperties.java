package org.uniprot.api.uniref.common.repository.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@ConfigurationProperties(prefix = "voldemort.uniref.member")
@Data
public class UniRefMemberStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
    private int memberBatchSize;
    private int fetchMaxRetries;
    private int fetchRetryDelayMillis;
    private boolean brotliEnabled;
}
