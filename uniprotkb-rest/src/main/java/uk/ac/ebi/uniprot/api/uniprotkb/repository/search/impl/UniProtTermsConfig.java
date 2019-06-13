package uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 13/06/19
 *
 * @author Edd
 */
@Component
@Data
@ConfigurationProperties(prefix = "terms")
public class UniProtTermsConfig {
    private List<String> fields = new ArrayList<>();
}

