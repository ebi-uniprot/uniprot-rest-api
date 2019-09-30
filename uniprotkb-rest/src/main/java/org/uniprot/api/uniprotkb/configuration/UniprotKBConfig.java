package org.uniprot.api.uniprotkb.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.field.SearchField;
import org.uniprot.store.search.field.UniProtField;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler() {
        List<SearchField> boostFields =
                Arrays.stream(UniProtField.Search.values())
                        .filter(UniProtField.Search::hasBoostValue)
                        .collect(Collectors.toList());

        return new DefaultSearchHandler(
                UniProtField.Search.content, UniProtField.Search.accession, boostFields);
    }
}
