package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.uniprot.core.cv.ec.ECRepo;
import org.uniprot.core.cv.ec.ECRepoFactory;
import org.uniprot.core.cv.keyword.KeywordService;
import org.uniprot.core.cv.keyword.impl.KeywordServiceImpl;
import org.uniprot.core.cv.pathway.UniPathwayService;
import org.uniprot.core.cv.pathway.impl.UniPathwayServiceImpl;

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
    public ECRepo ecService() {
        return ECRepoFactory.get("");
    }

    @Bean
    public UniPathwayService pathwayService() {
        String filepath = "unipathway.txt";
        //        InputStream inputStream =
        // ViewByServiceConfig.class.getClassLoader().getResourceAsStream(filepath);
        //        if (inputStream != null) {
        //            filepath =
        // ViewByServiceConfig.class.getClassLoader().getResource(filepath).getFile();
        //        }
        return new UniPathwayServiceImpl(filepath);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public GoService goService(RestTemplate restTemplate) {
        return new GoService(restTemplate);
    }

    @Bean
    public UniProtViewByECService uniprotViewByECService(
            SolrClient solrClient, ConfigProperties configProperties, ECRepo ecRepo) {
        return new UniProtViewByECService(
                solrClient, configProperties.getUniprotCollection(), ecRepo);
    }

    @Bean
    public UniProtViewByKeywordService uniprotViewByKeywordService(
            SolrClient solrClient,
            ConfigProperties configProperties,
            KeywordService keywordService) {
        return new UniProtViewByKeywordService(
                solrClient, configProperties.getUniprotCollection(), keywordService);
    }

    @Bean
    public UniProtViewByPathwayService uniprotViewByPathwayService(
            SolrClient solrClient,
            ConfigProperties configProperties,
            UniPathwayService unipathwayService) {
        return new UniProtViewByPathwayService(
                solrClient, configProperties.getUniprotCollection(), unipathwayService);
    }

    @Bean
    public UniProtViewByGoService uniprotViewByGoService(
            SolrClient solrClient, ConfigProperties configProperties, GoService goService) {
        return new UniProtViewByGoService(
                solrClient, configProperties.getUniprotCollection(), goService);
    }

    @Bean
    public TaxonomyService taxonomyService(RestTemplate restTemplate) {
        return new TaxonomyService(restTemplate);
    }

    @Bean
    public UniProtViewByTaxonomyService uniProtViewByTaxonomyService(
            SolrClient solrClient,
            ConfigProperties configProperties,
            TaxonomyService taxonomyService) {
        return new UniProtViewByTaxonomyService(
                solrClient, configProperties.getUniprotCollection(), taxonomyService);
    }
}
