package org.uniprot.api.unisave.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@Data
@Builder
public class FullEntry {
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
    private String content;
}
