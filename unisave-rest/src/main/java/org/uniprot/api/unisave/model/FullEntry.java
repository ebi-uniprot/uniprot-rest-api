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
    private String name;
    private int entryVersion;
    private int sequenceVersion;
    private String database;
    private String firstRelease;
    private String firstReleaseDate;
    private String lastRelease;
    private String lastReleaseDate;
    private String content;
}
