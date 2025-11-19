package org.uniprot.api.support.data.common.taxonomy.repository;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "taxonomy.terms")
public class TaxonomyTermsConfig {
    private List<String> fields;
}
