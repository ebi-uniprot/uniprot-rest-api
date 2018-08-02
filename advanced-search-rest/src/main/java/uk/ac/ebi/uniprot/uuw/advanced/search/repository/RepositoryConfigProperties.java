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

    private String host;

    private String username;

    private String password;

/*    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }*/
}
