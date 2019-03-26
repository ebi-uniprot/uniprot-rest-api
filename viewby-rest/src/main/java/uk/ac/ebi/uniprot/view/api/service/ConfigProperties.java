package uk.ac.ebi.uniprot.view.api.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "solr")
public class ConfigProperties {
	private String uniprotCollection;
	//private String ecCollection;
}
