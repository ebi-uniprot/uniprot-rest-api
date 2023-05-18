package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.ec.ECRepoFactory;
import org.uniprot.cv.keyword.KeywordRepo;
import org.uniprot.cv.keyword.impl.KeywordRepoImpl;
import org.uniprot.cv.pathway.UniPathwayRepo;
import org.uniprot.cv.pathway.impl.UniPathwayRepoImpl;

@Configuration
public class ViewByServiceConfig {
    @Bean
    public ViewByConfigProperties configProperties() {
        return new ViewByConfigProperties();
    }

    @Bean
    public KeywordRepo keywordService(ViewByConfigProperties configProperties) {
        return new KeywordRepoImpl(configProperties.getKeywordFile());
    }

    @Bean
    public ECRepo ecService(ViewByConfigProperties configProperties) {
        return ECRepoFactory.get(configProperties.getEcDir());
    }

    @Bean
    public UniPathwayRepo pathwayService(ViewByConfigProperties configProperties) {
        return new UniPathwayRepoImpl(configProperties.getUniPathWayFile());
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
            KeywordRepo keywordRepo) {
        return new UniProtViewByKeywordService(
                solrClient, configProperties.getUniprotCollection(), keywordRepo);
    }

    @Bean
    public UniProtViewByPathwayService uniprotViewByPathwayService(
            SolrClient solrClient,
            ViewByConfigProperties configProperties,
            UniPathwayRepo unipathwayRepo) {
        return new UniProtViewByPathwayService(
                solrClient, configProperties.getUniprotCollection(), unipathwayRepo);
    }

    @Bean
    public UniProtViewByGoService uniprotViewByGoService(
            SolrClient solrClient, ViewByConfigProperties configProperties, GoService goService) {
        return new UniProtViewByGoService(
                solrClient, configProperties.getUniprotCollection(), goService);
    }

    @Bean
    public UniProtKBViewByTaxonomyService uniProtViewByTaxonomyService(
            SolrClient solrClient,
            ViewByConfigProperties configProperties,
            TaxonomyService taxonomyService) {
        return new UniProtKBViewByTaxonomyService(
                solrClient, configProperties.getUniprotCollection(), taxonomyService);
    }
}
