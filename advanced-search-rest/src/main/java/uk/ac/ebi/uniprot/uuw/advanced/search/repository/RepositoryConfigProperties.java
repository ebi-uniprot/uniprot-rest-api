package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Solr properties bean that will be injected with values from application.properties.
 *
 * @author lgonzales
 */
@Data
@ConfigurationProperties(prefix = "spring.data.solr")
public class RepositoryConfigProperties {

    private String zookeperhost;

    private String httphost;

    private String username;

    private String password;

}
