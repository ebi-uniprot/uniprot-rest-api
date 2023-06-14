package org.uniprot.api.uniprotkb.groupby;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "solr.groupby")
public class GroupByConfigProperties {
    private String ecDir;
}
