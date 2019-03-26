package uk.ac.ebi.uniprot.view.api.service;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.uniprot.cv.ec.ECService;
import uk.ac.ebi.uniprot.cv.ec.impl.ECServiceImpl;
import uk.ac.ebi.uniprot.cv.go.GoService;
import uk.ac.ebi.uniprot.cv.go.impl.GoServiceImpl;
import uk.ac.ebi.uniprot.cv.keyword.KeywordService;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordServiceImpl;
import uk.ac.ebi.uniprot.cv.pathway.UniPathwayService;
import uk.ac.ebi.uniprot.cv.pathway.impl.UniPathwayServiceImpl;


@Configuration
public class ViewByServiceConfig {
	  @Bean
	    public ConfigProperties configProperties() {
	        return new ConfigProperties();
	    }

	  @Bean
	    public KeywordService keywordService() {
	        return new KeywordServiceImpl();
	    }

	  
	  @Bean
	    public ECService ecService() {
	        return new ECServiceImpl("");
	    }
	  
	  @Bean
	    public UniPathwayService pathwayService() {
	        return new UniPathwayServiceImpl("src/main/resources/unipathway.txt");
	    }
	  
	  @Bean
		public RestTemplate restTemplate(RestTemplateBuilder builder) {
			return builder.build();
		}
	  
	  @Bean
	    public GoService goService() {
	        return new GoServiceImpl("src/main/resources/go");
	    }
	  
	    @Bean
	    public UniProtViewByECService uniprotViewByECService(SolrClient solrClient, ConfigProperties configProperties, ECService ecService) {
	        return new UniProtViewByECService(solrClient, configProperties.getUniprotCollection(), ecService );
	    }
	    
	    @Bean
	    public UniProtViewByKeywordService uniprotViewByKeywordService(SolrClient solrClient, ConfigProperties configProperties, KeywordService keywordService) {
	        return new UniProtViewByKeywordService(solrClient, configProperties.getUniprotCollection(), keywordService);
	    }
	    @Bean
	    public UniProtViewByPathwayService uniprotViewByPathwayService(SolrClient solrClient,
	    		ConfigProperties configProperties, UniPathwayService unipathwayService) {
	        return new UniProtViewByPathwayService(solrClient, configProperties.getUniprotCollection(), unipathwayService);
	    }
	    
	    @Bean
	    public UniProtViewByGoService uniprotViewByGoService(SolrClient solrClient,
	    		ConfigProperties configProperties, GoService goService) {
	        return new UniProtViewByGoService(solrClient, configProperties.getUniprotCollection(), goService);
	    }
	    
	    @Bean
	    public TaxonomyService taxonomyService(RestTemplate restTemplate) {
	        return new TaxonomyService( restTemplate);
	    }
	    
	    
	    @Bean
	    public UniProtViewByTaxonomyService uniProtViewByTaxonomyService(SolrClient solrClient, ConfigProperties configProperties,TaxonomyService taxonomyService) {
	        return new UniProtViewByTaxonomyService(solrClient, configProperties.getUniprotCollection(), taxonomyService);
	    }
	    
}
