package org.uniprot.api.support.data;

import static org.mockito.Mockito.mock;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniprotKBStatisticsEntryRepository;

@TestConfiguration
public class DataStoreTestConfig {

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient solrClient() {
        return mock(SolrClient.class);
    }

    @Bean("testUniprotkbStatisticsEntryRepository")
    @Profile("offline")
    public UniprotKBStatisticsEntryRepository uniprotkbStatisticsEntryRepository() {
        return mock(UniprotKBStatisticsEntryRepository.class);
    }

    @Bean("testStatisticsCategoryRepository")
    @Profile("offline")
    public StatisticsCategoryRepository statisticsCategoryRepository() {
        return mock(StatisticsCategoryRepository.class);
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter() {
            @Override
            public JsonQueryRequest toJsonQueryRequest(SolrRequest request) {
                JsonQueryRequest solrQuery = super.toJsonQueryRequest(request);

                // required for tests, because EmbeddedSolrServer is not sharded
                ((ModifiableSolrParams) solrQuery.getParams()).set("distrib", "false");
                ((ModifiableSolrParams) solrQuery.getParams()).set("terms.mincount", "1");

                return solrQuery;
            }
        };
    }
}
