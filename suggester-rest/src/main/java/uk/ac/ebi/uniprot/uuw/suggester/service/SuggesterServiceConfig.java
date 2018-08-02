package uk.ac.ebi.uniprot.uuw.suggester.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Arrays.asList;

/**
 * Provides beans used to give access to suggestions retrieved from a Solr instance.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@Configuration
public class SuggesterServiceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggesterServiceConfig.class);
    private static final String ID = "id";
    private static final String COMMA = ",";

    @Bean
    public SolrConfigProperties solrConfigProperties() {
        return new SolrConfigProperties();
    }

    @Bean
    public SolrClient createSolr(SolrConfigProperties solrConfigProperties) {
        String solrUrl = solrConfigProperties.getUrl();
        if (solrConfigProperties.isUseCloudClient()) {
            String[] solrUrls = solrUrl.split(COMMA);
            LOGGER.debug("Using CloudSolrClient (i.e., pointing to a Zookeeper) with url: {}", solrUrls);
            CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder().withZkHost(asList(solrUrls)).build();
            cloudSolrClient.setDefaultCollection(solrConfigProperties.getCollectionName());
            String idFieldName = solrConfigProperties.getIdFieldName();
            if (!idFieldName.equals(ID)) {
                cloudSolrClient.setIdField(idFieldName);
            }
            return cloudSolrClient;
        } else {
            LOGGER.debug("Using ConcurrentUpdateSolrClient with url: {}", solrUrl);
            return new ConcurrentUpdateSolrClient.Builder(solrUrl)
                    .withQueueSize(solrConfigProperties.getQueueSize())
                    .withThreadCount(solrConfigProperties.getThreadCount()).build();
        }
    }

    @Bean
    public SuggesterService suggesterService(SolrClient solrClient) {
        return new SuggesterService(solrClient);
    }
}
