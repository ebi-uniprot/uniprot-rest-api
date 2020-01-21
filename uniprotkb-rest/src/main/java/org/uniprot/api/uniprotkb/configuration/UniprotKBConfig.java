package org.uniprot.api.uniprotkb.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.SearchField;
import org.uniprot.store.search.field.UniProtField;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler() {
        List<SearchField> boostFields =
                Arrays.stream(UniProtField.Search.values())
                        .filter(UniProtField.Search::hasBoostValue)
                        .collect(Collectors.toList());

        return new DefaultSearchHandler(
                UniProtSearchFields.UNIPROTKB, "content", "accession", boostFields);
    }
}
