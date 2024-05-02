package org.uniprot.api.uniprotkb.common.repository.search;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Created 13/06/19
 *
 * @author Edd
 */
@Component
@Data
@ConfigurationProperties(prefix = "terms")
public class UniProtTermsConfig {
    private List<String> fields;
}
