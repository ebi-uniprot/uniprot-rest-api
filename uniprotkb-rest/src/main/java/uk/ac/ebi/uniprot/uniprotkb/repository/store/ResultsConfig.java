package uk.ac.ebi.uniprot.uniprotkb.repository.store;

import org.apache.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.common.repository.store.TupleStreamTemplate;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.DefaultEntryConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.impl.AvroByteArraySerializer;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.DefaultEntryObject;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.RepositoryConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.RepositoryConfigProperties;

import java.util.Base64;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Import(RepositoryConfig.class)
public class ResultsConfig {
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
        DefaultEntryConverter defaultEntryConverter = new DefaultEntryConverter();
        AvroByteArraySerializer<DefaultEntryObject> avroDeserializer =
                AvroByteArraySerializer.instanceOf(DefaultEntryObject.class);

        byte[] avroBinaryBytes = Base64.getDecoder().decode(s.getBytes());
        DefaultEntryObject avroObject = avroDeserializer.fromByteArray(avroBinaryBytes);
        return defaultEntryConverter.fromAvro(avroObject);
    }

}
