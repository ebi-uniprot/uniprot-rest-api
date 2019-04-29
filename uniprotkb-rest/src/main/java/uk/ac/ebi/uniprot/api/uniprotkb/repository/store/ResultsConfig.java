package uk.ac.ebi.uniprot.api.uniprotkb.repository.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import uk.ac.ebi.uniprot.api.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.api.common.repository.store.TupleStreamTemplate;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfigProperties;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.json.parser.uniprot.UniprotJsonConfig;

import java.io.IOException;
import java.util.Base64;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Import(RepositoryConfig.class)
public class ResultsConfig {
    private static final Logger LOGGER = getLogger(ResultsConfig.class);

    @Bean
    public TupleStreamTemplate cloudSolrStreamTemplate(RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return TupleStreamTemplate.builder()
                .collection("uniprot")
                .key("accession_id")
                .requestHandler("/export")
                .zookeeperHost(configProperties.getZkHost())
                .httpClient(httpClient)
                .build();
    }

    @Bean
    public StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer(UniProtStoreClient uniProtClient, TupleStreamTemplate tupleStreamTemplate) {
        return StoreStreamer.<UniProtEntry>builder()
                .id(resultsConfigProperties().getUniprot().getValueId())
                .defaultsField(resultsConfigProperties().getUniprot().getDefaultsField())
                .streamerBatchSize(resultsConfigProperties().getUniprot().getBatchSize())
                .storeClient(uniProtClient)
                .defaultsConverter(this::convertDefaultAvroToUniProtEntry)
                .tupleStreamTemplate(tupleStreamTemplate)
                .build();
    }

    @Bean
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    private UniProtEntry convertDefaultAvroToUniProtEntry(String s) {
        UniProtEntry result = null;
        try {
            ObjectMapper jsonMapper = UniprotJsonConfig.getInstance().getFullObjectMapper();
            result = jsonMapper.readValue(Base64.getDecoder().decode(s),UniProtEntry.class);
        } catch (IOException e) {
            LOGGER.error("Error converting DefaultAvro to UniProtEntry",e);
        }
        return result;
    }

}
