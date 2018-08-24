package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created 22/08/18
 *
 * @author Edd
 */
@ConfigurationProperties(prefix = "streamer")
@Data
public class StreamerConfigProperties {
    private StreamerProperties uniprot;

    @Data
    static class StreamerProperties {
        private int batchSize;
        private String valueId;
    }
}
