package org.uniprot.api.uniprotkb.common.service.protnlm;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.uniprotkb.common.repository.store.protnlm.ProtNLMStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Service
public class ProtNLMUniProtKBEntryService {

    private final ProtNLMStoreClient protNLMStoreClient;

    public ProtNLMUniProtKBEntryService(ProtNLMStoreClient protNLMStoreClient) {
        this.protNLMStoreClient = protNLMStoreClient;
    }

    public UniProtKBEntry getProtNLMEntry(String accession) {
        return this.protNLMStoreClient
                .getEntry(accession)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "No entry found for accession: " + accession));
    }
}
