package org.uniprot.api.uniprotkb.configuration;

import static java.util.Collections.emptyList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.field.UniProtSearchFields;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler() {
        return new DefaultSearchHandler(
                UniProtSearchFields.UNIPROTKB, "content", "accession", emptyList());
    }
}
