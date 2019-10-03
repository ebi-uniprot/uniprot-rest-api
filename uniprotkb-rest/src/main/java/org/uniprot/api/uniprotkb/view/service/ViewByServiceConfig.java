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
    public ViewByConfigProperties configProperties() {
        return new ViewByConfigProperties();
    }

    @Bean
    public KeywordService keywordService(ViewByConfigProperties configProperties) {
        return new KeywordServiceImpl(configProperties.getKeywordFile());
    }

    @Bean
    public ECRepo ecService(ViewByConfigProperties configProperties) {
        return ECRepoFactory.get(configProperties.getEcDir());
    }

    @Bean
    public UniPathwayService pathwayService(ViewByConfigProperties configProperties) {
        return new UniPathwayServiceImpl(configProperties.getUniPathWayFile());
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
            SolrClient solrClient, ViewByConfigProperties configProperties, ECRepo ecRepo) {
        return new UniProtViewByECService(
                solrClient, configProperties.getUniprotCollection(), ecRepo);
    }

    @Bean
    public UniProtViewByKeywordService uniprotViewByKeywordService(
            SolrClient solrClient,
            ViewByConfigProperties configProperties,
            KeywordService keywordService) {
        return new UniProtViewByKeywordService(
                solrClient, configProperties.getUniprotCollection(), keywordService);
    }

    @Bean
    public UniProtViewByPathwayService uniprotViewByPathwayService(
            SolrClient solrClient,
            ViewByConfigProperties configProperties,
            UniPathwayService unipathwayService) {
        return new UniProtViewByPathwayService(
                solrClient, configProperties.getUniprotCollection(), unipathwayService);
    }

    @Bean
    public UniProtViewByGoService uniprotViewByGoService(
            SolrClient solrClient, ViewByConfigProperties configProperties, GoService goService) {
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
            ViewByConfigProperties configProperties,
            TaxonomyService taxonomyService) {
        return new UniProtViewByTaxonomyService(
                solrClient, configProperties.getUniprotCollection(), taxonomyService);
    }
}
