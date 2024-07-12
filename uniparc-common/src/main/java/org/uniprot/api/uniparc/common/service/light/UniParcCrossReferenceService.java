package org.uniprot.api.uniparc.common.service.light;

import net.jodah.failsafe.RetryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.request.UniParcDatabasesRequest;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntryLight;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class UniParcCrossReferenceService {

    private final UniParcLightStoreClient uniParcLightStoreClient;
    private final UniParcCrossReferenceStoreClient crossReferenceStoreClient;
    private final RetryPolicy<Object> crossReferenceStoreRetryPolicy;

    @Autowired
    public UniParcCrossReferenceService(UniParcLightStoreClient uniParcLightStoreClient,
                                        UniParcCrossReferenceStoreClient crossReferenceStoreClient,
                                        UniParcCrossReferenceStoreConfigProperties storeConfigProperties){
        this.uniParcLightStoreClient = uniParcLightStoreClient;
        this.crossReferenceStoreClient = crossReferenceStoreClient;
        this.crossReferenceStoreRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(storeConfigProperties.getFetchRetryDelayMillis()))
                        .withMaxRetries(storeConfigProperties.getFetchMaxRetries());
    }

    public QueryResult<UniParcCrossReference> getCrossReferences(String uniParcId, UniParcDatabasesRequest request){
        Optional<UniParcEntryLight> optUniParcLight = this.uniParcLightStoreClient.getEntry(uniParcId);
        if(optUniParcLight.isEmpty()){
            throw new ResourceNotFoundException("Unable to find UniParc id " + uniParcId);
        }
        UniParcEntryLight entry = optUniParcLight.get();
        List<String> xrefIds = entry.getUniParcCrossReferences();
        CursorPage page = CursorPage.of(request.getCursor(), request.getSize(), xrefIds.size());
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        List<String> xrefIdsPage = xrefIds.subList(offset, nextOffset);
        //TODO add filtering by dbTypes, active and taxonIds
        return QueryResult.<UniParcCrossReference>builder()
                .content(getCrossReferences(xrefIdsPage))
                .page(page)
                .build();
    }

    private Stream<UniParcCrossReference> getCrossReferences(List<String> xrefIds) {
        BatchStoreIterable<UniParcCrossReference> batchIterable =
                new BatchStoreIterable<>(
                        xrefIds,
                        this.crossReferenceStoreClient,
                        this.crossReferenceStoreRetryPolicy,
                        this.crossReferenceStoreClient.getBatchSize());
        return StreamSupport.stream(batchIterable.spliterator(), false)
                .flatMap(Collection::stream);
    }
}
