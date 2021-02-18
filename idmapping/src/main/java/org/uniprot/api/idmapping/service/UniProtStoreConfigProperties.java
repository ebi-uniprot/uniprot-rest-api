package org.uniprot.api.idmapping.service;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
// FIXME move it common used by uniprot kb rest as well
@ConfigurationProperties(prefix = "voldemort.uniprot")
@Data
public class UniProtStoreConfigProperties {
    private String host;
    private int numberOfConnections;
    private String storeName;
}
