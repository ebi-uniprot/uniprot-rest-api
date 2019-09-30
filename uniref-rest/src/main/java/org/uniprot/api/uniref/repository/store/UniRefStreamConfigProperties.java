package org.uniprot.api.uniref.repository.store;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jluo
 * @date: 21 Aug 2019
 */
@ConfigurationProperties(prefix = "streamer.uniref")
@Data
public class UniRefStreamConfigProperties {
    private int batchSize;
    private String valueId;
}
