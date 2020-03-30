package org.uniprot.api.unisave.service;

import org.uniprot.api.unisave.model.*;
import org.uniprot.api.unisave.request.UniSaveRequest;

import java.util.List;
import java.util.Optional;

public interface UniSaveService {
    Optional<UniSaveEntry> getEntryWithVersion(String accession, int version);

    List<UniSaveEntry> getEntries(String accession);

    DiffInfo getDiff(String accession, int version1, int version2);
    UniSaveEntry getDiff2(String accession, int version1, int version2);

    Optional<EntryInfo> getEntryInfoWithVersion(String accession, int version);

    List<EntryInfo> getEntryInfos(String accession);

    String convertToFasta(UniSaveEntry entry);

    Optional<AccessionStatus> getAccessionStatus(String accession);
    UniSaveEntry getAccessionStatus2(String accession);

    ReleaseInfo getLatestRelease();

    List<UniSaveEntry> getEntries(UniSaveRequest request);
}
