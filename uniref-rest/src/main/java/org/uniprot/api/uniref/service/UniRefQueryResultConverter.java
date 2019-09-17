package org.uniprot.api.uniref.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.api.common.repository.search.QueryResult;
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

public class UniRefQueryResultConverter {
	
	    private final UniRefStoreClient entryStore;
	    private final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
	             .handle(IOException.class)          
	             .withDelay(Duration.ofMillis(100))
	            .withMaxRetries(5);

	    UniRefQueryResultConverter(UniRefStoreClient entryStore) {
	        this.entryStore = entryStore;
	    }

	    QueryResult<UniRefEntry> convertQueryResult(QueryResult<UniRefDocument> results, Map<String, List<String>> filters) {
	        List<UniRefEntry> upEntries = results.getContent()
	                .stream().map(doc -> convertDoc(doc, filters))
	                .filter(Optional::isPresent)
	                .map(Optional::get)
	                .collect(Collectors.toList());
	        return QueryResult.of(upEntries, results.getPage(), results.getFacets(), results.getMatchedFields());
	    }

	    Optional<UniRefEntry> convertDoc(UniRefDocument doc, Map<String, List<String>> filters) {
	    	return Failsafe.with(retryPolicy).get(() ->entryStore.getEntry(doc.getId()));
	    }

}

