package uk.ac.ebi.uniprot.uniprotkb.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.uniprot.rest.search.DefaultSearchHandler;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler(){
        return new DefaultSearchHandler(UniProtField.Search.content,
                UniProtField.Search.accession
                ,UniProtField.getBoostFields());
    }
}
