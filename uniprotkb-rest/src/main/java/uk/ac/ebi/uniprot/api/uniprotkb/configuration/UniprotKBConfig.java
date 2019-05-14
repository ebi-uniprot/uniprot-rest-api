package uk.ac.ebi.uniprot.api.uniprotkb.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.field.SearchField;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.uniprot.search.field.UniProtField.Search;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler(){
        List<SearchField> boostFields = Arrays.stream(Search.values())
                .filter(Search::hasBoostValue)
                .collect(Collectors.toList());
        return new DefaultSearchHandler(Search.content, Search.accession,boostFields);
    }
}
