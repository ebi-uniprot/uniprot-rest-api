package org.uniprot.api.uniprotkb.repository.search.impl;

import java.util.List;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
