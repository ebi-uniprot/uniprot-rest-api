package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@ConfigurationProperties(prefix = "voldemort")
@Data
public class StoreConfigProperties {
    private String url;
}
