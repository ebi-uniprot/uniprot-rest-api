package org.uniprot.api.uniprotkb.view.service;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "solr.viewby")
public class ViewByConfigProperties {
    private String uniprotCollection;
    private String ecDir;
    private String uniPathWayFile;
}
