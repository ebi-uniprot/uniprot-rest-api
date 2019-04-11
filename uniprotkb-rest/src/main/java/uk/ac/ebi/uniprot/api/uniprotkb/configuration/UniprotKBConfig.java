package uk.ac.ebi.uniprot.api.uniprotkb.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.field.UniProtField;

@Configuration
public class UniprotKBConfig {

    @Bean
    public DefaultSearchHandler defaultSearchHandler(){
        return new DefaultSearchHandler(UniProtField.Search.content,
                UniProtField.Search.accession
                ,UniProtField.getBoostFields());
    }
}
