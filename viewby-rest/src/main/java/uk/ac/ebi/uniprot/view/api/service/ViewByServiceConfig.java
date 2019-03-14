package uk.ac.ebi.uniprot.view.api.service;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ViewByServiceConfig {
	  @Bean
	    public ConfigProperties configProperties() {
	        return new ConfigProperties();
	    }

	    @Bean
	    public UniProtViewByService uniprotViewByService(SolrClient solrClient, ConfigProperties configProperties) {
	        return new UniProtViewByService(solrClient, configProperties.getUniprotCollection());
	    }
}
