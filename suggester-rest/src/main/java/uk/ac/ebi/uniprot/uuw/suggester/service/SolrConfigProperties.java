package uk.ac.ebi.uniprot.uuw.suggester.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Solr properties bean that will be injected with values from application.properties.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@Data
@ConfigurationProperties(prefix = "solr")
public class SolrConfigProperties {
    private String collectionName;
}
