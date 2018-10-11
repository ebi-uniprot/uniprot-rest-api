package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.DefaultEntryConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.impl.AvroByteArraySerializer;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.DefaultEntryObject;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.RepositoryConfigProperties;
import uk.ac.ebi.uniprot.uuw.advanced.search.store.UniProtStoreClient;

import java.util.Base64;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class ResultsConfig {
    @Bean
    public TupleStreamTemplate cloudSolrStreamTemplate(RepositoryConfigProperties configProperties) {
        return TupleStreamTemplate.builder()
                .collection("uniprot")
                .key("accession")
                .order(SolrQuery.ORDER.asc)
                .requestHandler("/export")
                .zookeeperHost(configProperties.getZookeperhost())
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

    private UniProtEntry convertDefaultAvroToUniProtEntry(String s) {
        DefaultEntryConverter defaultEntryConverter = new DefaultEntryConverter();
        AvroByteArraySerializer<DefaultEntryObject> avroDeserializer =
                AvroByteArraySerializer.instanceOf(DefaultEntryObject.class);

        byte[] avroBinaryBytes = Base64.getDecoder().decode(s.getBytes());
        DefaultEntryObject avroObject = avroDeserializer.fromByteArray(avroBinaryBytes);
        return defaultEntryConverter.fromAvro(avroObject);
    }

    @Bean
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

}
