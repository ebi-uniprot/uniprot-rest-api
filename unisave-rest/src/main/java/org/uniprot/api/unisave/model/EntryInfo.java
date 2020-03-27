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
    private String database;
    private int entryVersion;
    private String firstRelease;
    private String firstReleaseDate;
    private String lastRelease;
    private String lastReleaseDate;
    private String md5;
    private String name;
    private int sequenceVersion;
    private boolean deleted;
    private String deletedReason;
    private List<String> mergedTo;
    private List<String> replacingAcc;
}
