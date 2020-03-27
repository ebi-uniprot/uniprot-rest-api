package org.uniprot.api.unisave.service;

import com.google.inject.ImplementedBy;
import org.uniprot.api.unisave.model.AccessionStatus;
import org.uniprot.api.unisave.model.EntryInfo;
import org.uniprot.api.unisave.model.FullEntry;
import org.uniprot.api.unisave.model.ReleaseInfo;
import org.uniprot.api.unisave.repository.domain.Diff;
import org.uniprot.api.unisave.service.impl.UniSaveServiceImpl;

import java.util.List;
import java.util.Optional;

@ImplementedBy(UniSaveServiceImpl.class)
public interface UniSaveService {
    Optional<FullEntry> getEntryWithVersion(String accession, int version);

    List<FullEntry> getEntries(String accession);

    Diff getDiff(String accession, int version1, int version2);

    Optional<EntryInfo> getEntryInfoWithVersion(String accession, int version);

    List<EntryInfo> getEntryInfos(String accession);

    String convertToFasta(FullEntry entry);

    Optional<AccessionStatus> getAccessionStatus(String accession);

    ReleaseInfo getLatestRelease();
}
