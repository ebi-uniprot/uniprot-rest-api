package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created 22/08/18
 *
 * @author Edd
 */
@ConfigurationProperties(prefix = "store")
@Data
public class ResultsConfigProperties {
    private int uniProtStreamerBatchSize;
    private String uniProtStreamerValueId;
}
