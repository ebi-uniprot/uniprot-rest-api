package org.uniprot.api.unisave.repository.domain;

import java.util.Date;

/**
 * Represent a release of uniprot.
 *
 * @author wudong
 */
public interface Release {

    /**
     * The entry iterator on all the entries in the release.
     *
     * @return
     */
    // Iterator<Entry> getEntryIterator();

    /**
     * The number of entry in the release.
     *
     * @return
     */
    // long getEntryCount();

    /**
     * The release number.
     *
     * @return
     */
    String getReleaseNumber();

    /**
     * The public release date of this release.
     *
     * @return
     */
    Date getReleaseDate();

    /**
     * The URI of the release, normally it will be the path to the release files.
     *
     * @return
     */
    String getReleaseURI();

    /**
     * What database this release belong to.
     *
     * @return
     */
    DatabaseEnum getDatabase();

    /**
     * The release's stats, which can only be obtained after the release finished.
     *
     * @return
     */
    ReleaseStats getStatus();
}
