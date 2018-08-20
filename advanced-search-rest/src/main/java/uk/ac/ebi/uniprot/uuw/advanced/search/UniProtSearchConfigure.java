package uk.ac.ebi.uniprot.uuw.advanced.search;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.dataaccess.EntryJsonDataAdapterImpl;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.ClientFactory;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.UniProtClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.impl.DefaultClientFactory;

@Configuration
public class UniProtSearchConfigure {
	@Bean
	public ClientFactory createVoldemortClientFactory(
			@Value("${voldemort.host}") String voldemortUrl,
			@Value("${voldemort.max.connection}") int numberOfConn,
			@Value("${voldemort.store}") String voldemortStore
			) {
		return new DefaultClientFactory( voldemortUrl, numberOfConn, voldemortStore);
	}
	@Bean
	public UniProtClient createVoldemortClient(ClientFactory factory) {
		return factory.createUniProtClient();
	}
	@Bean
	public JsonDataAdapter<UniProtEntry,UPEntry>  createUniProtJsonAdaptor() {
		return new EntryJsonDataAdapterImpl();
	}
}
