package org.uniprot.api.uniprotkb.common.service.precomputed;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Service
public class PrecomputedUniProtKBEntryService {

    private final PrecomputedAnnotationStoreClient precomputedAnnotationStoreClient;

    public PrecomputedUniProtKBEntryService(
            PrecomputedAnnotationStoreClient precomputedAnnotationStoreClient) {
        this.precomputedAnnotationStoreClient = precomputedAnnotationStoreClient;
    }

    public UniProtKBEntry getPrecomputedUniProtKBEntry(String upi, String taxId) {
        String precomputedEntryId = upi + "-" + taxId;
        return this.precomputedAnnotationStoreClient
                .getEntry(precomputedEntryId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "No precomputed entry found for id: "
                                                + precomputedEntryId));
    }
}
