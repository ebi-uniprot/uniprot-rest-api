package org.uniprot.api.uniref.repository.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 *
 * @author jluo
 * @date: 21 Aug 2019
 *
*/
@ConfigurationProperties(prefix = "streamer.uniref")
@Data
public class UniRefStreamConfigProperties {
	private int batchSize;
	private String valueId;
}

