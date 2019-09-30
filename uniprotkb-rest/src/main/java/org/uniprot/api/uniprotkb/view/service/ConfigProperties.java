package org.uniprot.api.uniprotkb.view.service;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "solr")
public class ConfigProperties {
    private String uniprotCollection;
}
