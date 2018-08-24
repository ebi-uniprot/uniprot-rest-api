package uk.ac.ebi.uniprot.uuw.advanced.search.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.dataaccess.EntryJsonDataAdapterImpl;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;

@Configuration
public class UniProtSearchConfigure {
//	@Bean
//	public ClientFactory createVoldemortClientFactory(
//			@Value("${voldemort.host}") String voldemortUrl,
//			@Value("${voldemort.max.connection}") int numberOfConn,
//			@Value("${voldemort.store}") String voldemortStore
//			) {
//		return new DefaultClientFactory( voldemortUrl, numberOfConn, voldemortStore);
//	}
//	@Bean
//	public UniProtClient createVoldemortClient(ClientFactory factory) {
//		return factory.createUniProtClient();
//	}
	@Bean
	public JsonDataAdapter<UniProtEntry,UPEntry>  createUniProtJsonAdaptor() {
		return new EntryJsonDataAdapterImpl();
	}
}
