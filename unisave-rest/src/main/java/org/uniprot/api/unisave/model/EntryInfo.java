package org.uniprot.api.unisave.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@Data
@Builder
public class EntryInfo {
    private String accession;
    private int entryVersion;
    private int sequenceVersion;
    private String entryMD5;
    private String database;
    private String firstRelease;
    private String firstReleaseDate;
    private String lastRelease;
    private String lastReleaseDate;
    private List<String> replacingAcc;
    private boolean deleted;
    private String deletedReason;
    private List<String> mergedTo;
}
