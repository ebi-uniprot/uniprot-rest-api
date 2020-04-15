package org.uniprot.api.unisave.repository.domain;

/**
 * The Entry object that doesn't contain a content.
 *
 * @author wudong
 */
public interface BasicEntryInfo {

    /**
     * The database of this entry, either swissprot or trembl.
     *
     * @return
     */
    DatabaseEnum getDatabase();

    String getAccession();

    String getName();

    int getSequenceVersion();

    int getEntryVersion();

    String getEntryMD5();

    String getSequenceMD5();

    Release getFirstRelease();

    Release getLastRelease();
}
