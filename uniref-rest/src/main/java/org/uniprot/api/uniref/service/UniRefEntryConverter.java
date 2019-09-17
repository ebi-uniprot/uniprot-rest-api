package org.uniprot.api.uniref.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.api.uniref.repository.store.UniRefStoreClient;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.document.uniref.UniRefDocument;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 *
 * @author jluo
 * @date: 20 Aug 2019
 *
*/

public class UniRefEntryConverter implements Function<UniRefDocument, UniRefEntry> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UniRefEntryConverter.class);

	
	  private final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
	             .handle(IOException.class)          
	             .withDelay(Duration.ofMillis(100))
	            .withMaxRetries(5);
	  private final UniRefStoreClient entryStore;
	  
	public UniRefEntryConverter(UniRefStoreClient entryStore) {
		this.entryStore = entryStore;
	}


	@Override
	public UniRefEntry apply(UniRefDocument doc) {
		 Optional<UniRefEntry> opEntry =Failsafe.with(retryPolicy).get(() ->entryStore.getEntry(doc.getId()));
		 if(opEntry.isPresent()) {
			 return opEntry.get();
		 }else {
			 LOGGER.info("Failing to fetch UniRef Entry: " + doc.getId());
		 }
		 return null;
		 
	}

}

