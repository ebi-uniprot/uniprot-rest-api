package org.uniprot.api.unisave.repository.domain;

import java.util.List;

public interface EntryInfo extends BasicEntryInfo {
    List<String> getReplacingAccession();

    boolean isDeleted();

    String getDeletionReason();

    List<String> getMergingTo();
}
