package uk.ac.ebi.uniprot.api.uniprotkb.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.UniProtField;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler(){
        List<SearchField> boostFields = Arrays.stream(UniProtField.Search.values())
                .filter(UniProtField.Search::hasBoostValue)
                .collect(Collectors.toList());

        return new DefaultSearchHandler(UniProtField.Search.content,
                UniProtField.Search.accession
                ,boostFields);
    }
}
