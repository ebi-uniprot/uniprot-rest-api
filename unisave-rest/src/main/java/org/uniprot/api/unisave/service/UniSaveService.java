package org.uniprot.api.unisave.service;

import java.util.List;

import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.request.UniSaveRequest;

public interface UniSaveService {
    UniSaveEntry getDiff(String accession, int version1, int version2);

    UniSaveEntry getAccessionStatus(String accession);

    List<UniSaveEntry> getEntries(UniSaveRequest.Entries request);
}
