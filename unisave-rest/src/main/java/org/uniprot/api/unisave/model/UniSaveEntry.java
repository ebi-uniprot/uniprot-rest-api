package org.uniprot.api.unisave.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UniSaveEntry {
    private String accession;
    private String database;
    private Integer entryVersion;
    private String firstRelease;
    private String firstReleaseDate;
    private String lastRelease;
    private String lastReleaseDate;
    private String name;
    private Integer sequenceVersion;
    private String content;
    private Boolean deleted;
    private String deletedReason;
    private List<String> mergedTo;
    private List<String> replacingAcc;
    private DiffInfo diffInfo;
    private AccessionStatus status;
    private List<AccessionEvent> events;
    private boolean isCurrentRelease;

    @JsonIgnore
    public boolean isCurrentRelease() {
        return isCurrentRelease;
    }

    @JsonIgnore
    private Integer
            entryVersionUpper; // used only when constructing unique sequence aggregates in fasta
    // format

    public static class UniSaveEntryBuilder {
        public String getLastRelease() {
            return lastRelease;
        }

        public String getFirstReleaseDate() {
            return firstReleaseDate;
        }

        public String getFirstRelease() {
            return firstRelease;
        }

        public String getContent() {
            return content;
        }
    }
}
