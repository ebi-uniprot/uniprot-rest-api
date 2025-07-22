package org.uniprot.api.uniprotkb.common.service.protlm;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.uniprotkb.common.repository.store.protlm.ProtLMStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Service
public class ProtLMUniProtKBEntryService {

    private final ProtLMStoreClient protLMStoreClient;

    public ProtLMUniProtKBEntryService(ProtLMStoreClient protLMStoreClient) {
        this.protLMStoreClient = protLMStoreClient;
    }

    public UniProtKBEntry getProtLMEntry(String accession) {
        Optional<UniProtKBEntry> entryOpt = this.protLMStoreClient.getEntry(accession);
        if (entryOpt.isEmpty()) {
            throw new ResourceNotFoundException("No entry found for accession: " + accession);
        }
        return entryOpt.get();
    }
}
