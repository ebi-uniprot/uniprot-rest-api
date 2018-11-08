package uk.ac.ebi.uniprot.uniprotkb.repository.store;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.ac.ebi.uniprot.common.repository.store.StoreStreamer;

/**
 * This class represents configurable properties of {@link StoreStreamer} instances.
 *
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
        private String defaultsField;
    }
}
