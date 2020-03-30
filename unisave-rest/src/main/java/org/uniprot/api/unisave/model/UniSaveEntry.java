package org.uniprot.api.unisave.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UniSaveEntry {
    private String accession;
    private String database;
    private Integer entryVersion;
    private String firstRelease;
    private String firstReleaseDate;
    private String lastRelease;
    private String lastReleaseDate;
    private String md5;
    private String name;
    private Integer sequenceVersion;
    private String content;
    private Boolean deleted;
    private String deletedReason;
    private List<String> mergedTo;
    private List<String> replacingAcc;
    private DiffInfo diffInfo;
    private AccessionStatus status;

    public static class UniSaveEntryBuilder {
        public String getLastRelease() {
            return lastRelease;
        }

        public String getFirstReleaseDate() {
            return firstReleaseDate;
        }

        public String getContent() {
            return content;
        }
    }

    public static void main(String[] args) {
        String thing = UniSaveEntry.builder().lastRelease("thing").getLastRelease();
        System.out.println(thing);
    }
}
